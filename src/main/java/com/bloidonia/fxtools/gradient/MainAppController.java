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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
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

public class MainAppController {
    @FXML private VBox rootPane ;
    @FXML private ImageView imageView ;
    @FXML private Canvas canvas ;
    @FXML private Pane previewPane ;
    @FXML private TextArea cssOutput ;
    
    GraphicsContext gc = null ;
    private double startX;
    private double startY;
    
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
    
    // Convert a JavaFX Color to an rgb int
    private static int toRGB( Color c ) {
        return ( ( (int)( c.getRed() * 255 ) & 0xFF ) << 16 ) |
               ( ( (int)( c.getGreen() * 255 ) & 0xFF ) << 8  ) |
               ( ( (int)( c.getBlue() * 255 ) & 0xFF ) ) ;
    }

    // Breshenham integer line drawing to get the pixels between two points
    // This needs improving to deal with sub-pixels    
    private List<Integer> getIntGraph( int x1, int y1, int x2, int y2 ) {
        if( x2 > imageView.getImage().getWidth() - 1 ) { x2 = (int)imageView.getImage().getWidth() - 1 ; }
        if( y2 > imageView.getImage().getHeight() - 1 ) { y2 = (int)imageView.getImage().getHeight() - 1 ; }
        int dx = (int)Math.abs( x2 - x1 ) ;
        int sx = x1 < x2 ? 1 : -1 ;
        int dy = (int)Math.abs( y2 - y1 ) ;
        int sy = y1 < y2 ? 1 : -1 ;
        int err = ( dx > dy ? dx : -dy ) / 2 ;
        int e2 ;

        PixelReader pr = imageView.getImage().getPixelReader() ;
        
        List<Integer> colors = new ArrayList<>() ;
        while( true ) {
            Color color = pr.getColor( x1, y1 ) ;
            int icolor = toRGB( color ) ;
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
    
    private String hexify( int color ) {
        return String.format( "#%6s", Integer.toHexString( color ) ).replace( ' ', '0' ) ;
    }
    
    // Finds the indexes of peaks in the Red, Green or Blue channel
    private List<Integer> findPeaks( List<Integer> colors, int shift ) {
        Integer previous = null;
        Integer previousSlope = 0;

        List<Integer> ret = new ArrayList<>() ;

        for( int i = 0 ; i < colors.size() ; i++ ) {
            if( previous == null ) {
                previous = ( colors.get( i ) >> shift ) & 0xFF ;
                continue ;
            }
            Integer p = ( colors.get( i ) >> shift ) & 0xFF ;
            Integer slope = p - previous ;
            if( slope * previousSlope < 0 ) {
                ret.add( i ) ;
            }
            previousSlope = slope ;
            previous = p ;
        }
        return ret ;
    }
    
    private void generateCss( double x1, double y1, double x2, double y2 ) {
        if( x2 < 0 || x2 >= imageView.getImage().getWidth() ||
            y2 < 0 || y2 >= imageView.getImage().getHeight() ) {
            return ;
        }
        List<Integer> colors = getIntGraph( (int)x1, (int)y1, (int)x2, (int)y2 ) ;

        // Red peak indexes
        Set<Integer> peaks = new TreeSet<>( findPeaks( colors, 16 ) ) ;
        // Green peak indexes
        peaks.addAll( findPeaks( colors, 8 ) ) ;
        // Blue peak indexes
        peaks.addAll( findPeaks( colors, 0 ) ) ;

        StringBuilder css = new StringBuilder().append( "-fx-background-color:\n" ) ;
        css.append( String.format( "    linear-gradient( to right,\n" ) )
           .append( String.format( "                     %s 0%%,\n", hexify( colors.get( 0 ) ) ) ) ;
        peaks.stream().forEach( ( pos ) -> {
            css.append( String.format( "                     %s %.2f%%,\n", hexify( colors.get( pos ) ), ( (double)pos / colors.size() ) * 100.0d ) ) ;
        } );
        css.append( String.format( "                     %s 100%% )", hexify( colors.get( colors.size() - 1 ) ) ) ) ;

        cssOutput.setText( css.toString() ) ;
        previewPane.setStyle( css.toString() );
    }
    
    @FXML private void handlePressedAction( MouseEvent event ) {
        clearCanvas() ;
        startX = event.getX() ;
        startY = event.getY() ;
    }

    @FXML private void handleDragAction( MouseEvent event ) {
        clearCanvas() ;
        lineTo( event.getX(), event.getY() ) ;
        generateCss( startX, startY, event.getX(), event.getY() ) ;
    }

    @FXML private void handleReleasedAction( MouseEvent event ) {
        generateCss( startX, startY, event.getX(), event.getY() ) ;
    }
}
