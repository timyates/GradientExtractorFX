### GradientExtractorFX

- Requires Java 8

Run with:

    ./gradlew run

Then, click `Load Image`, and drag a line across it to generate a JavaFX css linear gradient

### Current Screenshot

![](https://raw2.github.com/timyates/GradientExtractorFX/28f823d5cf79ac6006c7bc829aaa82a96bf53f7d/screenshot.png)

### Known Issues

- Sometimes gets the colors wrong (get color abberation esp on diagonal drag lines)
- Sometimes misses peaks, so color steps are missing
- Just discovered you can only have 12 stops in a linear gradient without throwing an exception (but it seems to keep working)... Needs investigating

### TODO

I think sub-pixel accuracy would improve both of these
