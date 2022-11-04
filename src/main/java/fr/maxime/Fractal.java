package fr.maxime;

import org.lwjgl.BufferUtils;
import org.lwjgl.Version;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.stb.STBTTAlignedQuad;
import org.lwjgl.stb.STBTTPackContext;
import org.lwjgl.stb.STBTTPackedchar;
import org.lwjgl.system.MemoryStack;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static fr.maxime.IOUtil.ioResourceToByteBuffer;
import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.stb.STBTruetype.*;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.*;

public class Fractal {

    // The window handle
    private long window;

    // Taille de la fenêtre
    public static final int WIDTH = 1280;
    public static final int HEIGHT = 720;

    // Propriétés

    public static float zoom = 1;
    public static float moveX = 0;
    public static float moveY = 0;
    public static float real = 0.25f;//-0.2f;//-0.7f;
    public static float imaginary = 0;//0.7f;//0.27025f;

    // Souris
    double xMousePos;
    double yMousePos;

    // Font
    private int font = 5;
    private boolean supportsSRGB;
    private int font_tex;
    private STBTTPackedchar.Buffer chardata;
    private static final int BITMAP_W = 512;
    private static final int BITMAP_H = 512;
    private static final float[] scale = {
            24.0f,
            14.0f
    };
    private final STBTTAlignedQuad q = STBTTAlignedQuad.malloc();
    private final FloatBuffer xb = memAllocFloat(1);
    private final FloatBuffer yb = memAllocFloat(1);

    public void run() {
        System.out.println("Hello LWJGL " + Version.getVersion() + "!");

        init();
        load_fonts();
        loop();

        GL.setCapabilities(null);

        chardata.free();

        // Free the window callbacks and destroy the window
        glfwFreeCallbacks(window);
        glfwDestroyWindow(window);

        // Terminate GLFW and free the error callback
        glfwTerminate();
        glfwSetErrorCallback(null).free();

        memFree(yb);
        memFree(xb);

        q.free();
    }

    private void init() {
        // Setup an error callback. The default implementation
        // will print the error message in System.err.
        GLFWErrorCallback.createPrint(System.err).set();

        // Initialize GLFW. Most GLFW functions will not work before doing this.
        if (!glfwInit())
            throw new IllegalStateException("Unable to initialize GLFW");

        // Configure GLFW
        glfwDefaultWindowHints(); // optional, the current window hints are already the default
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE); // the window will stay hidden after creation
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE); // the window will be resizable

        // Create the window
        // Utiliser window = glfwCreateWindow(WIDTH, HEIGHT, "Poissons", glfwGetPrimaryMonitor(), NULL); pour le plein écran
        window = glfwCreateWindow(WIDTH, HEIGHT, "Poissons", NULL, NULL);
        if (window == NULL)
            throw new RuntimeException("Failed to create the GLFW window");

        glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_HIDDEN);

        // Setup a key callback. It will be called every time a key is pressed, repeated or released.
        glfwSetKeyCallback(window, (window, key, scancode, action, mods) -> {
            if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE)
                glfwSetWindowShouldClose(window, true); // We will detect this in the rendering loop

            if (action != GLFW_PRESS) {
                switch (key) {
                    case GLFW_KEY_EQUAL -> zoom *= 1.01f;
                    case GLFW_KEY_6 -> zoom /= 1.1f;
                    case GLFW_KEY_LEFT -> moveX -= 0.1f/zoom;
                    case GLFW_KEY_RIGHT -> moveX += 0.1f/zoom;
                    case GLFW_KEY_DOWN -> moveY -= 0.1f/zoom;
                    case GLFW_KEY_UP -> moveY += 0.1f/zoom;
                    case GLFW_KEY_R -> real += 0.0001f;
                    case GLFW_KEY_E -> real -= 0.0001f;
                    case GLFW_KEY_I -> imaginary += 0.0005f;
                    case GLFW_KEY_U -> imaginary -= 0.0005f;
                    case GLFW_KEY_D -> {
                        real += 0.0001f;
                        imaginary += 0.0005f;
                    }
                    case GLFW_KEY_S -> {
                        real -= 0.0001f;
                        imaginary -= 0.0005f;
                    }
                }
                System.out.println(real + " " + imaginary);

                if(action == GLFW_RELEASE){
                    switch (key){
                        case GLFW_KEY_0 -> {
                            zoom = 1;
                            moveX = 0;
                            moveY = 0;
                        }
                        case GLFW_KEY_M -> {
                            if (glfwGetInputMode(window, GLFW_CURSOR) == GLFW_CURSOR_NORMAL) {
                                glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_HIDDEN);
                            } else {
                                glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_NORMAL);
                            }
                        }
                    }
                }

            }
        });

        /*
        glfwSetMouseButtonCallback(window, (window,button,action,mods) -> {
            if(button == GLFW_MOUSE_BUTTON_LEFT && action == GLFW_PRESS){

            }
        });

        glfwSetCursorPosCallback(window, (window,x,y) -> {
            xMousePos = x;
            yMousePos = y;
        });
         */

        // Get the thread stack and push a new frame
        try (MemoryStack stack = stackPush()) {
            IntBuffer pWidth = stack.mallocInt(1); // int*
            IntBuffer pHeight = stack.mallocInt(1); // int*

            // Get the window size passed to glfwCreateWindow
            glfwGetWindowSize(window, pWidth, pHeight);

            // Get the resolution of the primary monitor
            GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());

            // Center the window
            glfwSetWindowPos(
                    window,
                    (vidmode.width() - pWidth.get(0)) / 2,
                    (vidmode.height() - pHeight.get(0)) / 2
            );
        } // the stack frame is popped automatically

        // Make the OpenGL context current
        glfwMakeContextCurrent(window);
        GL.createCapabilities();
        // Enable v-sync
        glfwSwapInterval(1);

        // Make the window visible
        glfwShowWindow(window);

        // Detect sRGB support
        GLCapabilities caps = GL.getCapabilities();
        supportsSRGB = caps.OpenGL30 || caps.GL_ARB_framebuffer_sRGB || caps.GL_EXT_framebuffer_sRGB;
    }

    private void load_fonts() {
        font_tex = glGenTextures();
        chardata = STBTTPackedchar.malloc(6 * 128);

        try (STBTTPackContext pc = STBTTPackContext.malloc()) {
            ByteBuffer ttf = ioResourceToByteBuffer("font/FiraSans-Thin.ttf", 512 * 1024);

            ByteBuffer bitmap = BufferUtils.createByteBuffer(BITMAP_W * BITMAP_H);

            stbtt_PackBegin(pc, bitmap, BITMAP_W, BITMAP_H, 0, 1, NULL);
            for (int i = 0; i < 2; i++) {
                int p = (i * 3 + 0) * 128 + 32;
                chardata.limit(p + 95);
                chardata.position(p);
                stbtt_PackSetOversampling(pc, 1, 1);
                stbtt_PackFontRange(pc, ttf, 0, scale[i], 32, chardata);

                p = (i * 3 + 1) * 128 + 32;
                chardata.limit(p + 95);
                chardata.position(p);
                stbtt_PackSetOversampling(pc, 2, 2);
                stbtt_PackFontRange(pc, ttf, 0, scale[i], 32, chardata);

                p = (i * 3 + 2) * 128 + 32;
                chardata.limit(p + 95);
                chardata.position(p);
                stbtt_PackSetOversampling(pc, 3, 1);
                stbtt_PackFontRange(pc, ttf, 0, scale[i], 32, chardata);
            }
            chardata.clear();
            stbtt_PackEnd(pc);

            glBindTexture(GL_TEXTURE_2D, font_tex);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_ALPHA, BITMAP_W, BITMAP_H, 0, GL_ALPHA, GL_UNSIGNED_BYTE, bitmap);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void loop() {
        // This line is critical for LWJGL's interoperation with GLFW's
        // OpenGL context, or any context that is managed externally.
        // LWJGL detects the context that is current in the current thread,
        // creates the GLCapabilities instance and makes the OpenGL
        // bindings available for use.
        GL.createCapabilities();

        Shader shader = new Shader("fractal");

        float[] vertices = new float[]{
                -1f, -1f,
                1f, -1f,
                1f, 1f,

                1f, 1f,
                -1f, 1f,
                -1f, -1f,
        };

        float[] colors = new float[]{
                0f, 7.f / 255.f, 100.f / 255.f,
                32.f / 255.f, 107.f / 255.f, 203.f / 255.f,
                237.f / 255.f, 1.0f, 1.0f,
                1.0f, 170.f / 255.f, 0f,
                0f, 2.f / 255.f, 0f,
                0f, 7.f / 255.f, 100.f / 255.f,
        };

        Model model = new Model(vertices);

        // Run the rendering loop until the user has attempted to close
        // the window or has pressed the ESCAPE key.
        while (!glfwWindowShouldClose(window)) {
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // clear the framebuffer

            update();
            shader.bind();
            shader.setUniformi("width", WIDTH);
            shader.setUniformi("height", HEIGHT);
            shader.setUniformf("zoom", zoom);
            shader.setUniformf("moveX", moveX);
            shader.setUniformf("moveY", moveY);
            shader.setUniformf("maxIter", 100);
            shader.setUniformf("real", real);
            shader.setUniformf("imaginary", imaginary);
            //model.render();
            render();

            glfwSwapBuffers(window); // swap the color buffers

            // Poll for window events. The key callback above will only be
            // invoked during this call.
            glfwPollEvents();
        }
    }

    private void update() {

    }

    private void render() {
        /*
        // Set the clear color
        glClearColor(0.0f, 0.0f, 0.0f, 0.0f);

        glDisable(GL_CULL_FACE);
        glDisable(GL_TEXTURE_2D);
        glDisable(GL_LIGHTING);
        glDisable(GL_DEPTH_TEST);
        glViewport(0, 0, WIDTH, HEIGHT);
        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();
        glOrtho(0.0, WIDTH, HEIGHT, 0.0, -1.0, 1.0);
        glMatrixMode(GL_MODELVIEW);
        glLoadIdentity();
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        */

        glBegin(GL_QUADS);
        glVertex2i(-WIDTH/2,-HEIGHT/2);
        glVertex2i(-WIDTH/2, HEIGHT/2);
        glVertex2i(WIDTH/2, HEIGHT/2);
        glVertex2i(WIDTH/2, -HEIGHT/2);
        glEnd();
    }

    public static void main(String[] args) {
        new Fractal().run();
    }

}
