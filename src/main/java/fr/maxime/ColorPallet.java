package fr.maxime;

public enum ColorPallet {

    ORIGINAL(new float[]{
            0.f / 255.f, 7.f / 255.f, 100.f / 255.f,
            32.f / 255.f, 107.f / 255.f, 203.f / 255.f,
            237.f / 255.f, 1.0f, 1.0f,
            1.0f, 170.f / 255.f, 0.f / 255.f,
            0.f / 255.f, 2.f / 255.f, 0.f / 255.f,
            0.f / 255.f, 7.f / 255.f, 100.f / 255.f,
    }, 6),

    FIRE(new float[]{
            20.f / 255.f, 0.f / 255.f, 0.f / 255.f,
            255.f / 255.f, 20.f / 255.f, 0.f / 255.f,
            255.f / 255.f, 200.f / 255.f, 0.f / 255.f,
            255.f / 255.f, 20.f / 255.f, 0.f / 255.f,
            20.f / 255.f, 0.f / 255.f, 0.f / 255.f,
    }, 5),

    ELECTRIC(new float[]{
            0.f / 255.f, 0.f / 255.f, 0.f / 255.f,
            0.f / 255.f, 0.f / 255.f, 200.f / 255.f,
            255.f / 255.f, 255.f / 255.f, 255.f / 255.f,
            0.f / 255.f, 0.f / 255.f, 200.f / 255.f,
            0.f / 255.f, 0.f / 255.f, 0.f / 255.f,
    }, 5),

    GOLD(new float[]{
            85.f / 255.f, 47.f / 255.f, 0.f / 255.f,
            255.f / 255.f, 171.f / 255.f, 12.f / 255.f,
            255.f / 255.f, 247.f / 255.f, 127.f / 255.f,
            255.f / 255.f, 171.f / 255.f, 12.f / 255.f,
            85.f / 255.f, 47.f / 255.f, 0.f / 255.f,
    }, 5),

    RGB_GRADIENT(new float[]{
            255.f / 255.f, 0.f / 255.f, 0.f / 255.f,
            255.f / 255.f, 255.f / 255.f, 0.f / 255.f,
            0.f / 255.f, 255.f / 255.f, 0.f / 255.f,
            0.f / 255.f, 255.f / 255.f, 255.f / 255.f,
            0.f / 255.f, 0.f / 255.f, 255.f / 255.f,
            255.f / 255.f, 0.f / 255.f, 255.f / 255.f,
            255.f / 255.f, 0.f / 255.f, 0.f / 255.f,
    }, 7),

    RGB(new float[]{
            255.f / 255.f, 0.f / 255.f, 0.f / 255.f,
            255.f / 255.f, 255.f / 255.f, 0.f / 255.f,
            0.f / 255.f, 255.f / 255.f, 0.f / 255.f,
            0.f / 255.f, 255.f / 255.f, 255.f / 255.f,
            0.f / 255.f, 0.f / 255.f, 255.f / 255.f,
            255.f / 255.f, 0.f / 255.f, 255.f / 255.f,
            255.f / 255.f, 0.f / 255.f, 0.f / 255.f,
    }, 7),

    BLACK_AND_WHITE(new float[]{
            0.f / 255.f, 0.f / 255.f, 0.f / 255.f,
            255.f / 255.f, 255.f / 255.f, 255.f / 255.f,
            0.f / 255.f, 0.f / 255.f, 0.f / 255.f,
    }, 3),

    SET_ONLY(new float[]{
            255.f / 255.f, 255.f / 255.f, 255.f / 255.f,
            255.f / 255.f, 255.f / 255.f, 255.f / 255.f,
    }, 2);

    private final float[] colors;
    private final int nbColors;

    ColorPallet(float[] colors, int nbColors) {
        this.colors = colors;
        this.nbColors = nbColors;
    }

    public float[] getColors(){
        return colors;
    }

    public int getNbColors(){
        return nbColors;
    }

}