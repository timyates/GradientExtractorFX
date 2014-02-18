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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.BlendMode;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;

/**
 * @author Tim Yates
 */
public class GraphPane extends Pane {
    private Canvas view = new Canvas( 200, 48 ) ;
    private ListProperty<RGB> pixels ;
    private ListProperty<Integer> peaks ;
    
    public GraphPane() {
        this.widthProperty().addListener( (ObservableValue<? extends Number> obs,Number oldW,Number newW) -> {
            GraphPane.this.getChildren().clear() ;
            GraphPane.this.getChildren().add( GraphPane.this.view = new Canvas( newW.intValue(), 48 ) ) ;
            update() ;
        } ) ;
    }

    public List<RGB> getPixels() {
        return pixels == null ? new ArrayList<>() : pixels.get() ;
    }

    public ListProperty pixelsProperty() {
        if( pixels == null ) {
            pixels = new SimpleListProperty() ;
            pixels.addListener( this::updatePixels ) ;
        }
        return pixels ;
    }

    public List<Integer> getPeaks() {
        return peaks == null ? new ArrayList<>() : peaks.get() ;
    }
    
    public ListProperty peaksProperty() {
        if( peaks == null ) {
            peaks = new SimpleListProperty() ;
            peaks.addListener( this::updatePeaks ) ;
        }
        return peaks ;
    }

    void updatePixels( ObservableValue<? extends ObservableList<RGB>> a,
                  ObservableList<RGB> b,
                  ObservableList<RGB> pixels ) {
        update() ;
    }

    void updatePeaks( ObservableValue<? extends ObservableList<Integer>> a,
              ObservableList<Integer> b,
              ObservableList<Integer> peaks ) {
        update() ;
    }

    private void render( GraphicsContext g, double h, double dw, double dh, List<RGB> pixels, Function<RGB,Integer> fn ) {
        g.beginPath() ;
        g.moveTo( 0, h );
        for( int i = 0 ; i < pixels.size() ; i++ ) {
            g.lineTo( dw * i, h - fn.apply( pixels.get( i ) ) * dh ) ;
        }
        g.lineTo( this.widthProperty().get(), h );
        g.closePath() ;
        g.stroke() ;
        g.fill() ;
    }

    protected final void update() {
        if( pixels == null || peaks == null ) return ;
        GraphicsContext g = view.getGraphicsContext2D() ;
        g.setLineWidth( 1 ) ;
        double h = this.heightProperty().get() ;
        double w = this.widthProperty().get() ;
        g.clearRect( 0, 0, w, h );
        double dh = h / 255.0 ;
        double dw = w / ( pixels.size() - 1 ) ;
        g.setStroke( Color.RED ) ;
        g.setFill( Color.rgb( 255, 0, 0, 0.2 ) ) ;
        render( g, h, dw, dh, pixels, (rgb) -> rgb.getR() ) ;
        g.setStroke( Color.GREEN ) ;
        g.setFill( Color.rgb( 0, 255, 0, 0.2 ) ) ;
        render( g, h, dw, dh, pixels, (rgb) -> rgb.getG() ) ;
        g.setStroke( Color.BLUE ) ;
        g.setFill( Color.rgb( 0, 0, 255, 0.2 ) ) ;
        render( g, h, dw, dh, pixels, (rgb) -> rgb.getB() ) ;
        g.setLineWidth( 2 ) ;
        BlendMode mode = g.getGlobalBlendMode() ;
        g.setGlobalBlendMode( BlendMode.DIFFERENCE );
        g.setFill( Color.PINK ) ;
        for( Integer peak : peaks ) {
            g.fillRect( ( peak * dw ) - 1, 0, 3, h );
        }
        g.setGlobalBlendMode( mode );
    }    
}
