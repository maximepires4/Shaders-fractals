package fr.maxime;

import org.apache.commons.cli.*;
import org.lwjgl.Version;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.glfw.GLFWWindowSizeCallback;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryStack;

import java.nio.IntBuffer;
import java.util.Locale;

import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;

public class Fractal {

    // Arguments
    private final String fractalType;
    private final ColorPallet colorPallet;

    // Window handle
    private long window;
    private GLFWWindowSizeCallback glfwWindowSizeCallback;
    public final boolean fullscreen;

    // Window size
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
    private boolean mouse = false;
    private double xMousePos;
    private double yMousePos;

    public Fractal(String fractalType, ColorPallet colorPallet, boolean fullscreen){
        this.fractalType = fractalType;
        this.colorPallet = colorPallet;
        this.fullscreen = fullscreen;
    }

    public Fractal(String fractalType, boolean fullscreen){
        this.fractalType = fractalType;
        this.colorPallet = ColorPallet.ORIGINAL;
        this.fullscreen = fullscreen;
    }

    public Fractal(ColorPallet colorPallet, boolean fullscreen){
        this.fractalType = "mandelbrot";
        this.colorPallet = colorPallet;
        this.fullscreen = fullscreen;
    }

    public Fractal(boolean fullscreen){
        this.fractalType = "mandelbrot";
        this.colorPallet = ColorPallet.ORIGINAL;
        this.fullscreen = fullscreen;
    }

    public void run() {
        System.out.println("Hello LWJGL " + Version.getVersion() + "!");

        init();
        loop();

        GL.setCapabilities(null);

        glfwWindowSizeCallback.close();

        // Free the window callbacks and destroy the window
        glfwFreeCallbacks(window);
        glfwDestroyWindow(window);

        // Terminate GLFW and free the error callback
        glfwTerminate();
        glfwSetErrorCallback(null).free();
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

        if(fullscreen){
            monitor = glfwGetPrimaryMonitor();
            GLFWVidMode mode = glfwGetVideoMode(monitor);
            width = mode.width();
            height = mode.height();
        }

        window = glfwCreateWindow(width, height, "Fractals", monitor, NULL);

        if (window == NULL)
            throw new RuntimeException("Failed to create the GLFW window");

        glfwSetInputMode(window, GLFW_CURSOR, mouse ? GLFW_CURSOR_NORMAL : GLFW_CURSOR_HIDDEN);

        // Setup a key callback. It will be called every time a key is pressed, repeated or released.
        glfwSetKeyCallback(window, (window, key, scancode, action, mods) -> {
            if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE)
                glfwSetWindowShouldClose(window, true); // We will detect this in the rendering loop

            if(action != GLFW_REPEAT) {
                switch(key){
                    case GLFW_KEY_EQUAL, GLFW_KEY_KP_ADD -> zoomToggle = action == GLFW_PRESS ? 1 : 0;
                    case GLFW_KEY_6, GLFW_KEY_KP_SUBTRACT -> zoomToggle = action == GLFW_PRESS ? -1 : 0;
                    case GLFW_KEY_Z -> {
                        if(action == GLFW_RELEASE) zoomToggle = (zoomToggle+1)%2;
                    }
                    case GLFW_KEY_LEFT -> moveXToggle = action == GLFW_PRESS ? -1 : 0;
                    case GLFW_KEY_RIGHT -> moveXToggle = action == GLFW_PRESS ? 1 : 0;
                    case GLFW_KEY_DOWN -> moveYToggle = action == GLFW_PRESS ? -1 : 0;
                    case GLFW_KEY_UP -> moveYToggle = action == GLFW_PRESS ? 1 : 0;
                    case GLFW_KEY_0 -> {
                        if(action == GLFW_RELEASE){
                            zoom = 1;
                            moveX = 0;
                            moveY = 0;
                        }
                    }
                    case GLFW_KEY_M -> {
                        if(action == GLFW_RELEASE){
                            mouse = !mouse;
                            xGoal = moveX;
                            yGoal = moveY;
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

        if(!fullscreen){
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
    }

    private void loop() {
        // This line is critical for LWJGL's interoperation with GLFW's
        // OpenGL context, or any context that is managed externally.
        // LWJGL detects the context that is current in the current thread,
        // creates the GLCapabilities instance and makes the OpenGL
        // bindings available for use.
        GL.createCapabilities();

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

        shader = new Shader(fractalType);
        model = new Model(vertices, indices);

        int targetUps = 60;
        int targetFps = 60;

        long initialTime = System.currentTimeMillis();
        float timeU = 1000.0f / targetUps;
        float timeR = 1000.0f / targetFps;
        float deltaUpdate = 0;
        float deltaFps = 0;

        long updateTime = initialTime;

        // Run the rendering loop until the user has attempted to close
        // the window or has pressed the ESCAPE key.
        while (!glfwWindowShouldClose(window)) {
            // Poll for window events. The key callback above will only be
            // invoked during this call.
            glfwPollEvents();

            long now = System.currentTimeMillis();
            deltaUpdate += (now - initialTime) / timeU;
            deltaFps += (now - initialTime) / timeR;

            if(deltaUpdate >= 1){
                long diffTimeMillis = now - updateTime;
                update();
                updateTime = now;
                deltaUpdate--;
            }

            if(deltaFps >= 1){
                render();
                deltaFps--;
                glfwSwapBuffers(window); // swap the color buffers
            }

            initialTime = now;
        }
    }

    private void update() {
        if(zoomToggle == 1){
            zoom *= 1.05d;
        } else if(zoomToggle == -1){
            zoom /= 1.05d;
        }

        moveX += 0.05d / zoom * moveXToggle;
        moveY += 0.05d / zoom * moveYToggle;

        if(mouse){
            moveX += 0.05 * (xGoal-moveX);
            moveY += 0.05 * (yGoal-moveY);
        }

        shader.setUniformi("width", width);
        shader.setUniformi("height", height);
        shader.setUniformd("zoom", zoom);
        shader.setUniformd("moveX", moveX);
        shader.setUniformd("moveY", moveY);
        shader.setUniformi("maxIter", maxIter);
        shader.setUniformf("real", real);
        shader.setUniformf("imaginary", imaginary);
        shader.setUniformfv("colors", colorPallet.getColors());
        shader.setUniformi("nb_colors", colorPallet.getNbColors());
    }

    private void render() {
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // clear the framebuffer

        glViewport(0, 0, width, height);

        shader.bind();
        model.render();
    }


    public static void main(String[] args) {
        Options options = new Options();

        Option fractalOpt = new Option("t", "type", true, "type of fractal (between mandelbrot or julia)");
        Option colorOpt = new Option("c", "color", true, "color palette");
        Option fullscreenOpt = new Option("f", "fullscreen", false, "fullscreen");

        options.addOption(fractalOpt);
        options.addOption(colorOpt);
        options.addOption(fullscreenOpt);

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd = null;

        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("utility-name", options);

            System.exit(1);
        }

        String fractalType = cmd.getOptionValue(fractalOpt);

        if(fractalType != null && !fractalType.equals("mandelbrot") && !fractalType.equals("julia")) {
            System.out.println("No fractal \"" + fractalType + "\", using mandelbrot");
            fractalType = null;
        }

        String color = cmd.getOptionValue(colorOpt);
        ColorPallet colorPallet = null;

        if(color != null){
            try {
                colorPallet = ColorPallet.valueOf(color.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e){
                System.out.println("No color \"" + color + "\", using original");
            }
        }

        boolean fullscreen = cmd.hasOption(fullscreenOpt);

        if(fractalType != null && colorPallet != null)
            new Fractal(fractalType, colorPallet, fullscreen).run();
        else if(fractalType != null)
            new Fractal(fractalType, fullscreen).run();
        else if(colorPallet != null)
            new Fractal(colorPallet, fullscreen).run();
        else
            new Fractal(fullscreen).run();
    }

}
