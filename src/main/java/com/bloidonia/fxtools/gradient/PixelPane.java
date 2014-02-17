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
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

/**
 * @author Tim Yates
 */
public class PixelPane extends Region {
    private final ImageView view = new ImageView() ;
    private ListProperty<RGB> pixels ;
    
    public PixelPane() {
        view.setPreserveRatio( false ) ;
        view.fitWidthProperty().bind( this.widthProperty() ) ; 
        view.fitHeightProperty().bind( this.heightProperty() ) ; 

        HBox box = new HBox() ;
        box.getChildren().add( view ) ;
        HBox.setHgrow( view, Priority.ALWAYS ) ;
        this.getChildren().add( box ) ;
    }
    
    public List<RGB> getPixels() {
        return pixels == null ? new ArrayList<>() : pixels.get() ;
    }

    public void setPixels( List pixels ) {
        pixelsProperty().set( pixels ) ;
    }

    public ListProperty pixelsProperty() {
        if( pixels == null ) {
            pixels = new SimpleListProperty() ;
            pixels.addListener( this::updatePixels ) ;
        }
        return pixels ;
    }

     void updatePixels( ObservableValue<? extends ObservableList<RGB>> a,
                       ObservableList<RGB> b,
                       ObservableList<RGB> pixels ) {
         if( pixels.size() < 1 ) return ;
         WritableImage back = new WritableImage( pixels.size(), 1 ) ;
         PixelWriter pw = back.getPixelWriter() ;
         for( int i = 0 ; i < pixels.size() ; i++ ) {
             pw.setArgb( i, 0, pixels.get( i ).getColor() );
         }
         view.setImage( back ) ;
    }
}
