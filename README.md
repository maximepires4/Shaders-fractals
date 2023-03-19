# Shaders fractals

Generate `mandelbrot` or `julia` fractal with shaders.

## Options

* **-f, --fullscreen** : Toggle fullscreen

* **-t, --type** : Type of the fractal, choose between "mandelbrot" or "julia"

* **-c, --color** : Color palette used, see [colors](#colors)

## Commands (QWERTY keyboard)

### Keyboard

* `+`/`-` Zoom (numeric keypad supported)
* `↑←↓→`Move with arrows
* `0` Reset zoom and move
* `Z` Toggle infinite zoom
* `M` Toggle following mouse
* `R` Increase real part (default -0.7)
* `E` Decrease real part
* `I` Increase imaginary part (default 0.27025f)
* `U` Decrease imaginary part
* `H` Increase max iterations (default 300)
* `G` Decrease max iterations
* `D` Increase real and imaginary part
* `S` Decrease real and imaginary part

### Mouse click

Click for focusing the clicked area (needs mouse following with M).

Hint:

1. Move and zoom to a place you like
2. Press `M` to see your mouse
3. Click wherever you want
4. Press `0` to zoom out
5. Press `Z` and enjoy!!

## Colors

Simply type the name of the color.

1. original
2. fire
3. electric
4. gold
5. rgb_gradient
6. rgb
7. black_and_white
8. set_only