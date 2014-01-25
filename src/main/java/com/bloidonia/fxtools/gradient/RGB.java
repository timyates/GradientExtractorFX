/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.bloidonia.fxtools.gradient;

import javafx.scene.paint.Color;

/**
 *
 * @author tyates
 */
public class RGB {
    private final int r, g, b ;
    
    public RGB( int r, int g, int b ) {
        this.r = r ;
        this.g = g ;
        this.b = b ;
    }
    
    public RGB withBrightness( float c ) {
        return new RGB( (int)( r * c ), (int)( g * c ), (int)( b * c ) ) ;
    }

    public int getColor() {
        return ( ( r << 16 ) & 0xFF0000 ) |
               ( ( g << 8  ) & 0xFF00 ) |
               ( ( b       ) & 0xFF ) ;
    }
    
    public int getR() {
        return r ;
    }

    public int getG() {
        return g ;
    }

    public int getB() {
        return b ;
    }
    
    public static RGB fromFX( Color color ) {
        return new RGB( (int)( color.getRed() * 255 ) & 0xFF,
                        (int)( color.getGreen() * 255 ) & 0xFF,
                        (int)( color.getBlue() * 255 ) & 0xFF ) ;
    }
    
    @Override
    public String toString() {
        return String.format( "%02x%02x%02x", r, g, b ) ;
    }
    
    public static void main(String[] args) {
        System.out.println( new RGB( 128, 32, 16 ).toString() ) ;
    }
}
