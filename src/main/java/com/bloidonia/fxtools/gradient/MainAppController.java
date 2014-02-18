/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bloidonia.fxtools.gradient;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelReader;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javax.imageio.ImageIO;

/**
 * @author Tim Yates
 */

public class MainAppController implements Initializable {
    @FXML private VBox rootPane ;
    @FXML private ImageView imageView ;
    @FXML private Canvas canvas ;
    @FXML private Pane previewPane ;
    @FXML private TextArea cssOutput ;
    @FXML private TextArea codeOutput ;
    @FXML private PixelPane pixels ;
    @FXML private GraphPane graph ;
    @FXML private Slider threshold ;
    @FXML private Label thresholdValue ;

    GraphicsContext gc = null ;
    private double startX;
    private double startY;
    private double endX;
    private double endY;
    private static final String FOUR_SPACES = "    " ;
    private static final String TWENTYONE_SPACES = "                     " ;
    
    private final ObservableList<RGB> pixelList = FXCollections.observableArrayList() ;
    private final ObservableList<Integer> peakList = FXCollections.observableArrayList() ;
    private ListProperty<RGB> pixelProperty ;
    private ListProperty<Integer> peakProperty ;

    @FXML
    private void handleButtonAction( ActionEvent event ) {
        final FileChooser fileChooser = new FileChooser();
        FileChooser.ExtensionFilter extFilterPNG = new FileChooser.ExtensionFilter("PNG files (*.png)", "*.PNG");
        FileChooser.ExtensionFilter extFilterJPG = new FileChooser.ExtensionFilter("JPG files (*.jpg)", "*.JPG");
        fileChooser.getExtensionFilters().addAll(extFilterPNG, extFilterJPG);
        final File file = fileChooser.showOpenDialog( rootPane.getScene().getWindow() ) ;
             
        try {
            imageView.setImage( loadFXImage( file ) ) ;
            canvas.setWidth( imageView.getImage().getWidth() ) ;
            canvas.setHeight( imageView.getImage().getHeight() ) ;
            clearCanvas() ;
            gc.setStroke( Color.RED ) ;
            gc.setLineWidth(3) ;
        }
        catch( IOException ex ) {
            Logger.getLogger( MainAppController.class.getName() ).log( Level.SEVERE, null, ex );
        }
    }

    public ObservableList<RGB> getPixels() {
        return pixelList ;
    }

    public ListProperty<RGB> pixelsProperty() {
        if( pixelProperty == null ) {
            pixelProperty = new SimpleListProperty( pixelList ) ;
        }
        return pixelProperty ;
    }

    public ObservableList<Integer> getPeaks() {
        return peakList ;
    }

    public ListProperty<Integer> peaksProperty() {
        if( peakProperty == null ) {
            peakProperty = new SimpleListProperty<>( peakList ) ;
        }
        return peakProperty ;
    }

    private Image loadFXImage( File file ) throws IOException {
        return SwingFXUtils.toFXImage( ImageIO.read( file ), null );
    }

    private void ensureGC() {
        if( gc == null ) {
            gc = canvas.getGraphicsContext2D() ;
        }
    }

    private void clearCanvas() {
        ensureGC() ;
        gc.clearRect( 0, 0, canvas.getWidth(), canvas.getHeight() );
    }

    private void lineTo( double x, double y ) {
        ensureGC() ;
        gc.strokeLine( startX, startY, x, y ) ;
    } 
    
    // Breshenham integer line drawing to get the pixels between two points
    // This needs improving to deal with sub-pixels    
    private List<RGB> pixelsInLine( int x1, int y1, int x2, int y2 ) {
        int dx = (int)Math.abs( x2 - x1 ) ;
        int sx = x1 < x2 ? 1 : -1 ;
        int dy = (int)Math.abs( y2 - y1 ) ;
        int sy = y1 < y2 ? 1 : -1 ;
        int err = ( dx > dy ? dx : -dy ) / 2 ;
        int e2 ;

        PixelReader pr = imageView.getImage().getPixelReader() ;
        
        List<RGB> colors = new ArrayList<>() ;
        while( true ) {
            Color color = pr.getColor( x1, y1 ) ;
            RGB icolor = RGB.fromFX( color ) ;
            colors.add( icolor ) ;
            if( x1 == x2 && y1 == y2 ) break ;
            e2 = err ;
            if( e2 > -dx ) {
                err -= dy ;
                x1 += sx ;
            }
            if( e2 < dy ) {
                err += dx ;
                y1 += sy ;
            }
        }
        return colors ;
    }

    class Location {
        private final Integer location ;
        private final List<RGB> pixels ;
        
        public Location( Integer location, List<RGB> pixels ) {
            this.location = location ;
            this.pixels = pixels ;
        }
        
        public Integer getLocation() { return location ; }
        public List<RGB> getPixels() { return pixels ; }
        @Override
        public String toString() { return String.format( "[%d:%s]", location, pixels.toString() ) ; }
    }

    private List<Integer> findPeaks( List<RGB> colors ) {
        final float THRESHOLD = threshold.valueProperty().floatValue() ;

        return Stream.concat( Stream.of( 0 ),
                                  Stream.iterate( 0, i -> i + 1 )
                                        .limit( Math.max( colors.size() - 2, 0 ) )
                                        .map( i -> new Location( i, colors.stream().skip( i ).limit( 3 ).collect( Collectors.toList() ) ) )
                                        .filter( (Location l) -> {
                                            float dr = l.getPixels().get( 0 ).getR() + ( ( l.getPixels().get( 2 ).getR() - l.getPixels().get( 0 ).getR() ) * 0.5f ) ;
                                            float dg = l.getPixels().get( 0 ).getG() + ( ( l.getPixels().get( 2 ).getG() - l.getPixels().get( 0 ).getG() ) * 0.5f ) ;
                                            float db = l.getPixels().get( 0 ).getB() + ( ( l.getPixels().get( 2 ).getB() - l.getPixels().get( 0 ).getB() ) * 0.5f ) ;
                                            return Math.abs( l.getPixels().get( 1 ).getR() - dr ) > THRESHOLD ||
                                                   Math.abs( l.getPixels().get( 1 ).getG() - dg ) > THRESHOLD ||
                                                   Math.abs( l.getPixels().get( 1 ).getB() - db ) > THRESHOLD ;
                                        } )
                                        .map( l -> l.getLocation() ) ).collect( Collectors.toList() ) ;
    }

    private String buildCss( List<RGB> colors, List<Integer> peaks ) {
        return peaks.stream()
             .limit( Math.max( peaks.size() - 1, 1 ) )
             .map( (pos) ->
                 String.format( "%s#%s %.2f%%,\n",
                                TWENTYONE_SPACES,
                                colors.get( pos ).toString(),
                                ( (double)pos / colors.size() ) * 100.0d ) )
             .reduce( String.format( "-fx-background-color:\n%slinear-gradient( to right,\n",
                                     FOUR_SPACES ),
                      (a, b) -> a.concat( b ) )
             .concat( String.format( "%s#%s 100%% )",
                                     TWENTYONE_SPACES,
                                     colors.get( colors.size() - 1 ).toString() ) ) ;
    }

    private String buildCode( List<RGB> colors, List<Integer> peaks ) {
        return peaks.stream()
             .map( (pos) ->
                 String.format( "    new Stop(%.3f, Color.web(\"#%s\") ),\n",
                                ( (double)pos / colors.size() ),
                                colors.get( pos ).toString() ) )
             .reduce( String.format( "Stop[] stops = new Stop[] {\n",
                                     FOUR_SPACES ),
                      (a, b) -> a.concat( b ) )
             .concat( String.format( "    new Stop(1, Color.web(\"#%s\") )\n",
                                     colors.get( colors.size() - 1 ).toString() ) )
             .concat( "} ;\n" )
             .concat( "LinearGradient g = new LinearGradient( 0, 0, 1, 0, true, CycleMethod.NO_CYCLE, stops ) ;" ) ;
    }

    private void generateCss( double x1, double y1, double x2, double y2 ) {
        if( x2 < 0 || x2 >= imageView.getImage().getWidth() ||
            y2 < 0 || y2 >= imageView.getImage().getHeight() ) {
            return ;
        }
        pixelsProperty().setAll( pixelsInLine( (int)x1, (int)y1, (int)x2, (int)y2 ) ) ;
        peakList.setAll( findPeaks( pixelList ) ) ;
    }
    
    @FXML private void handlePressedAction( MouseEvent event ) {
        clearCanvas() ;
        startX = event.getX() ;
        startY = event.getY() ;
    }

    @FXML private void handleDragAction( MouseEvent event ) {
        clearCanvas() ;
        endX = event.getX() ;
        endY = event.getY() ;
        lineTo( endX, endY ) ;
        generateCss( startX, startY, endX, endY ) ;
    }

    @FXML private void handleReleasedAction( MouseEvent event ) {
        generateCss( startX, startY, event.getX(), event.getY() ) ;
    }

    void updateText( ObservableValue<? extends ObservableList<Integer>> a,
              ObservableList<Integer> b,
              ObservableList<Integer> peaks ) {
        if( peakList.size() > 0 ) {
            String css = buildCss( pixelList, peakList ) ;
            cssOutput.setText( css ) ;
            codeOutput.setText( buildCode( pixelList, peakList ) ) ;
            previewPane.setStyle( css ) ;
            cssOutput.requestFocus() ;
        }
        else {
            cssOutput.setText( "" ) ;
            previewPane.setStyle( "" );
        }
    }

    @Override
    public void initialize( URL url, ResourceBundle rb ) {
        peaksProperty().addListener( this::updateText ) ;
        threshold.valueProperty().addListener( (o, oldV, newV) -> {
            thresholdValue.setText( String.format( "%1.2f", newV ) ) ;
            if( startX != 0.0 ) {
                generateCss( startX, startY, endX, endY ) ;
            }
        } );
    }
}
