package fr.maxime;

import org.lwjgl.BufferUtils;
import org.lwjgl.Version;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.glfw.GLFWWindowSizeCallback;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL30;
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

    // Window handle
    private long window;
    private GLFWWindowSizeCallback glfwWindowSizeCallback;

    // Window size
    public static final boolean FULLSCREEN = false;
    private static int width = 1280;
    private static int height = 720;

    // Shader
    private Shader shader;
    private Model model;

    // Properties
    private double xGoal;
    private double yGoal;
    private int zoomToggle = 0;
    private int moveXToggle = 0;
    private int moveYToggle = 0;
    private double zoom = 1;
    private double moveX = 0;
    private double moveY = 0;
    private float real = -0.7f;
    private float imaginary = .27025f;
    private int maxIter = 300;

    // Mouse
    private boolean mouse = true;
    private double xMousePos;
    private double yMousePos;

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

        glfwWindowSizeCallback.close();

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
        // Set up an error callback. The default implementation
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
        long monitor = NULL;

        if(FULLSCREEN){
            monitor = glfwGetPrimaryMonitor();
            GLFWVidMode mode = glfwGetVideoMode(monitor);
            width = mode.width();
            height = mode.height();
        }

        window = glfwCreateWindow(width, height, "Fractals", monitor, NULL);

        if (window == NULL)
            throw new RuntimeException("Failed to create the GLFW window");

        //glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_HIDDEN);

        // Setup a key callback. It will be called every time a key is pressed, repeated or released.
        glfwSetKeyCallback(window, (window, key, scancode, action, mods) -> {
            if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE)
                glfwSetWindowShouldClose(window, true); // We will detect this in the rendering loop

            if(action != GLFW_REPEAT) {
                switch(key){
                    case GLFW_KEY_EQUAL -> zoomToggle = action == GLFW_PRESS ? 1 : 0;
                    case GLFW_KEY_6 -> zoomToggle = action == GLFW_PRESS ? -1 : 0;
                    case GLFW_KEY_LEFT -> moveXToggle = action == GLFW_PRESS ? -1 : 0;
                    case GLFW_KEY_RIGHT -> moveXToggle = action == GLFW_PRESS ? 1 : 0;
                    case GLFW_KEY_DOWN -> moveYToggle = action == GLFW_PRESS ? -1 : 0;
                    case GLFW_KEY_UP -> moveYToggle = action == GLFW_PRESS ? 1 : 0;
                    case GLFW_KEY_0 -> {
                        if(action == GLFW_RELEASE){
                            zoom = 1;
                            moveX = 0;
                            moveY = 0;
                            maxIter = 300;
                        }
                    }
                    case GLFW_KEY_M -> {
                        if(action == GLFW_RELEASE){
                            mouse = !mouse;
                            glfwSetInputMode(window, GLFW_CURSOR, mouse ? GLFW_CURSOR_NORMAL : GLFW_CURSOR_HIDDEN);
                        }
                    }
                }
            }

            if(action == GLFW_RELEASE || action == GLFW_REPEAT) {
                switch(key){
                    case GLFW_KEY_R -> real += 0.01f;
                    case GLFW_KEY_E -> real -= 0.01f;
                    case GLFW_KEY_I -> imaginary += 0.01f;
                    case GLFW_KEY_U -> imaginary -= 0.01f;
                    case GLFW_KEY_H -> maxIter++;
                    case GLFW_KEY_G -> maxIter--;
                    case GLFW_KEY_D -> {
                        real += 0.0001d;
                        imaginary += 0.0005d;
                    }
                    case GLFW_KEY_S -> {
                        real -= 0.0001d;
                        imaginary -= 0.0005d;
                    }
                }
            }

            /*
            if (action != GLFW_PRESS) {
                switch (key) {
                    case GLFW_KEY_EQUAL -> zoom *= 1.1d;
                    case GLFW_KEY_6 -> zoom /= 1.1d;
                    case GLFW_KEY_LEFT -> moveX -= 0.1d/zoom;
                    case GLFW_KEY_RIGHT -> moveX += 0.1d/zoom;
                    case GLFW_KEY_DOWN -> moveY -= 0.1d/zoom;
                    case GLFW_KEY_UP -> moveY += 0.1d/zoom;
                    case GLFW_KEY_R -> real += 0.0001f;
                    case GLFW_KEY_E -> real -= 0.0001f;
                    case GLFW_KEY_I -> imaginary += 0.0005f;
                    case GLFW_KEY_U -> imaginary -= 0.0005f;
                    case GLFW_KEY_M -> maxIter++;
                    case GLFW_KEY_L -> maxIter--;
                    case GLFW_KEY_D -> {
                        real += 0.0001d;
                        imaginary += 0.0005d;
                    }
                    case GLFW_KEY_S -> {
                        real -= 0.0001d;
                        imaginary -= 0.0005d;
                    }
                }

                if(action == GLFW_RELEASE){
                    switch (key){
                        case GLFW_KEY_0 -> {
                            zoom = 1;
                            moveX = 0;
                            moveY = 0;
                            maxIter = 300;
                        }
                    }


                }
            }
            */
        });


        glfwSetMouseButtonCallback(window, (window,button,action,mods) -> {
            if(button == GLFW_MOUSE_BUTTON_LEFT && action == GLFW_PRESS){
                xGoal = 1.5 * (xMousePos - width * 0.5)/ (0.5 * zoom * width) + moveX;
                yGoal = ((height-yMousePos) - height * 0.5)/(0.5 * zoom * height) + moveY;
            }
        });

        glfwSetCursorPosCallback(window, (window,x,y) -> {
            xMousePos = x;
            yMousePos = y;
        });

        glfwWindowSizeCallback = new GLFWWindowSizeCallback() {
            @Override
            public void invoke(long window, int w, int h) {
                width = w;
                height = h;
            }
        };

        glfwSetWindowSizeCallback(window, glfwWindowSizeCallback);

        if(!FULLSCREEN){
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
        }

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

        float[] colors = new float[]{
                0f, 7.f / 255.f, 100.f / 255.f,
                32.f / 255.f, 107.f / 255.f, 203.f / 255.f,
                237.f / 255.f, 1.0f, 1.0f,
                1.0f, 170.f / 255.f, 0f,
                0f, 2.f / 255.f, 0f,
                0f, 7.f / 255.f, 100.f / 255.f,
        };

        float[] vertices = new float[]{
                -1, 1, // TOP LEFT      0
                1, 1, // TOP RIGHT      1
                1, -1, // BOTTOM RIGHT  2
                -1, -1, // BOTTOM LEFT  3
        };

        int[] indices = new int[]{
                0,1,2,
                2,3,0
        };

        shader = new Shader("fractal");
        model = new Model(vertices, indices);

        // Run the rendering loop until the user has attempted to close
        // the window or has pressed the ESCAPE key.
        while (!glfwWindowShouldClose(window)) {
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // clear the framebuffer

            update();
            render();

            glfwSwapBuffers(window); // swap the color buffers

            // Poll for window events. The key callback above will only be
            // invoked during this call.
            glfwPollEvents();
        }
    }

    private void update() {
        if(zoomToggle == 1){
            zoom *= 1.1d;
        } else if(zoomToggle == -1){
            zoom /= 1.1d;
        }

        moveX += 0.05d / zoom * moveXToggle;
        moveY += 0.05d / zoom * moveYToggle;

        if(mouse){
            moveX += 0.05 * (xGoal-moveX);
            moveY += 0.05 * (yGoal-moveY);
            zoom *= 1.1d;
        }

        shader.setUniformi("width", width);
        shader.setUniformi("height", height);
        shader.setUniformd("zoom", zoom);
        shader.setUniformd("moveX", moveX);
        shader.setUniformd("moveY", moveY);
        shader.setUniformi("maxIter", maxIter);
        shader.setUniformf("real", real);
        shader.setUniformf("imaginary", imaginary);
        shader.setUniformfv("colors", ColorPallet.ORIGINAL.getColors());
        shader.setUniformi("nb_colors", ColorPallet.ORIGINAL.getNbColors());
    }

    private void render() {
        glViewport(0, 0, width, height);

        shader.bind();
        model.render();

        /*
        // Set the clear color
        glClearColor(0.0f, 0.0f, 0.0f, 0.0f);


        glDisable(GL_CULL_FACE);
        glDisable(GL_TEXTURE_2D);
        glDisable(GL_LIGHTING);
        glDisable(GL_DEPTH_TEST);
        glViewport(0, 0, width, height);
        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();
        glOrtho(0.0, width, height, 0.0, -1.0, 1.0);
        glMatrixMode(GL_MODELVIEW);
        glLoadIdentity();
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);


        /*
        glBegin(GL_QUADS);
        glColor4f(0,0,0,1);
        glVertex2i(-width/2,-height /2);
        glVertex2i(-width /2, height /2);
        glVertex2i(width /2, height /2);
        glVertex2i(width /2, -height /2);
        glEnd();

        /*
        glVertex2i(-width/2,-height /2);
        glVertex2i(-width /2, height /2);
        glVertex2i(width /2, height /2);
        glVertex2i(width /2, -height /2);
        */

        /*
        if (supportsSRGB) glEnable(GL30.GL_FRAMEBUFFER_SRGB);

        glColor4f(1,1,1, 1);
        print(10,100, 1, "Salut");

        if (supportsSRGB) glDisable(GL30.GL_FRAMEBUFFER_SRGB);
         */
    }

    private void print(float x, float y, int font, String text) {
        xb.put(0, x);
        yb.put(0, y);

        chardata.position(font * 128);

        glEnable(GL_TEXTURE_2D);
        glBindTexture(GL_TEXTURE_2D, font_tex);

        glBegin(GL_QUADS);
        for (int i = 0; i < text.length(); i++) {
            stbtt_GetPackedQuad(chardata, BITMAP_W, BITMAP_H, text.charAt(i), xb, yb, q, false);
            drawBoxTC(
                    q.x0(), q.y0(), q.x1(), q.y1(),
                    q.s0(), q.t0(), q.s1(), q.t1()
            );
        }
        glEnd();
    }

    private static void drawBoxTC(float x0, float y0, float x1, float y1, float s0, float t0, float s1, float t1) {
        glTexCoord2f(s0, t0);
        glVertex2f(x0, y0);
        glTexCoord2f(s1, t0);
        glVertex2f(x1, y0);
        glTexCoord2f(s1, t1);
        glVertex2f(x1, y1);
        glTexCoord2f(s0, t1);
        glVertex2f(x0, y1);
    }


    public static void main(String[] args) {
        new Fractal().run();
    }

}
