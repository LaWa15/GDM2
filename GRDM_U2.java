package gdm;




import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.ImageCanvas;
import ij.gui.ImageWindow;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Panel;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
     Opens an image window and adds a panel below the image
*/
public class GRDM_U2 implements PlugIn {

    ImagePlus imp; // ImagePlus object
	private int[] origPixels;
	private int width;
	private int height;
	
	
    public static void main(String args[]) {
		new ImageJ();
    	//IJ.open("/users/barthel/applications/ImageJ/_images/orchid.jpg");
		IJ.open("C:\\Users/laris/Pictures/orchid.jpg");
		
		GRDM_U2 pw = new GRDM_U2();
		pw.imp = IJ.getImage();
		pw.run("");
	}
    
    public void run(String arg) {
    	if (imp==null) 
    		imp = WindowManager.getCurrentImage();
        if (imp==null) {
            return;
        }
        CustomCanvas cc = new CustomCanvas(imp);
        
        storePixelValues(imp.getProcessor());
        
        new CustomWindow(imp, cc);
    }


    private void storePixelValues(ImageProcessor ip) {
    	width = ip.getWidth();
		height = ip.getHeight();
		
		origPixels = ((int []) ip.getPixels()).clone();
	}


	class CustomCanvas extends ImageCanvas {
    
        CustomCanvas(ImagePlus imp) {
            super(imp);
        }
    
    } // CustomCanvas inner class
    
    
    class CustomWindow extends ImageWindow implements ChangeListener {
         
        private JSlider jSliderBrightness;
		private JSlider jSliderContrast;
		private JSlider jSliderSaturation;
		private JSlider jSliderColor;
		private double brightness, color, saturation, contrast;

		CustomWindow(ImagePlus imp, ImageCanvas ic) {
            super(imp, ic);
            addPanel();
            contrast = 10;
            saturation = 10;
            color =90;
            
        }
    
        void addPanel() {
        	//JPanel panel = new JPanel();
        	Panel panel = new Panel();

            panel.setLayout(new GridLayout(4, 1));
            jSliderBrightness = makeTitledSilder("Helligkeit" ,-128, 128, 0);
            jSliderContrast = makeTitledSilder("Kontrast", 0, 100, 10);
            jSliderSaturation = makeTitledSilder("Farbsättigung", 0, 50, 10);
            jSliderColor = makeTitledSilder("Farbänderung", 0, 360, 90);
            
            panel.add(jSliderBrightness);
            panel.add(jSliderContrast);
            panel.add(jSliderSaturation);
            panel.add(jSliderColor);
            
            add(panel);
            
            pack();
         }
      
        private JSlider makeTitledSilder(String string, int minVal, int maxVal, int val) {
		
        	JSlider slider = new JSlider(JSlider.HORIZONTAL, minVal, maxVal, val );
        	Dimension preferredSize = new Dimension(width, 50);
        	slider.setPreferredSize(preferredSize);
			TitledBorder tb = new TitledBorder(BorderFactory.createEtchedBorder(), 
					string, TitledBorder.LEFT, TitledBorder.ABOVE_BOTTOM,
					new Font("Sans", Font.PLAIN, 11));
			slider.setBorder(tb);
			slider.setMajorTickSpacing((maxVal - minVal)/10 );
			slider.setPaintTicks(true);
			slider.addChangeListener(this);
			
			return slider;
		}
        
        private void setSliderTitle(JSlider slider, String str) {
			TitledBorder tb = new TitledBorder(BorderFactory.createEtchedBorder(),
				str, TitledBorder.LEFT, TitledBorder.ABOVE_BOTTOM,
					new Font("Sans", Font.PLAIN, 11));
			slider.setBorder(tb);
		}

		public void stateChanged( ChangeEvent e ){
			JSlider slider = (JSlider)e.getSource();

			if (slider == jSliderBrightness) {
				brightness = slider.getValue();
				String str = "Helligkeit " + brightness; 
				setSliderTitle(jSliderBrightness, str); 
			}
			
			if (slider == jSliderContrast) {
				contrast = slider.getValue();
				String str = "Kontrast " + contrast/10; 
				setSliderTitle(jSliderContrast, str); 
			}
			
			if (slider == jSliderSaturation) {
				saturation = slider.getValue();
				String str = "Farbsättigung " + saturation/10; 
				setSliderTitle(jSliderSaturation, str); 
			}
			
			if (slider == jSliderColor) {
				color = slider.getValue();
				String str = "Farbänderung " + color; 
				setSliderTitle(jSliderColor, str); 
			}
			
			changePixelValues(imp.getProcessor());
			
			imp.updateAndDraw();
		}

		
		private void changePixelValues(ImageProcessor ip) {
			
			// Array fuer den Zugriff auf die Pixelwerte
			int[] pixels = (int[])ip.getPixels();
			
			for (int y=0; y<height; y++) {
				for (int x=0; x<width; x++) {
					int pos = y*width + x;
					int argb = origPixels[pos];  // Lesen der Originalwerte 
					
					int r = (argb >> 16) & 0xff;
					int g = (argb >>  8) & 0xff;
					int b =  argb        & 0xff;
					
					
					// anstelle dieser drei Zeilen spÃ¤ter hier die Farbtransformation durchfÃ¼hren,
					// die Y Cb Cr -Werte verÃ¤ndern und dann wieder zurÃ¼cktransformieren
					
					double a[] = transformation(r,g,b);
					double Y = a[0];
					double Cb=a[1];
					double Cr=a[2];
					
					Y = Y+brightness;
					Y=((Y-128)*contrast/10)+128;
					//Sättigung
					Cb=Cb*saturation/10;
					Cr=Cr*saturation/10;
					//Farbänderung
					Cb=Cb*( Math.cos(Math.toRadians(color)) + Math.sin(Math.toRadians(color)));
					Cr = Cr*( Math.sin(Math.toRadians(color)) - Math.cos(Math.toRadians(color)));

					int rgb[] = retransformation(Y,Cb,Cr);
					int rn, gn, bn;
					rn = rgb[0];
					gn = rgb[1];
					bn = rgb[2];
					
					if(rn>255)rn=255;
					if(gn>255)gn=255;
					if(bn>255)bn=255;
					if(rn<0)rn=0;
					if(gn<0)gn=0;
					if(bn<0)bn=0;
					
					
					
					
					/*int rn, gn, bn;
					if(r+brightness>255)rn =255;
					else if(r+brightness<0)rn =0;
					else rn = (int) (r + brightness);
					
					if(g+brightness>255)gn =255;
					else if(g+brightness<0)gn =0;
					else gn = (int) (g + brightness);

					if(b+brightness>255)bn =255;
					else if(b+brightness<0)bn =0;
					else bn = (int) (b + brightness);*/
					
					
					// Hier muessen die neuen RGB-Werte wieder auf den Bereich von 0 bis 255 begrenzt werden
					
					pixels[pos] = (0xFF<<24) | (rn<<16) | (gn<<8) | bn;
				}
			}
		}
		
    } // CustomWindow inner class
    
    public double[] transformation(int r, int g, int b) {
		double Y = 0.299*r+ 0.587*g + 0.114*b;
		double Cb = -0.168736 * r - 0.331264 * g + 0.5 * b;
		double Cr = 0.5 * r - 0.418688 * g - 0.081312 * b;
		double a[] = {Y,Cb,Cr};
		return  a;
    	   }
    
    public int[] retransformation(double Y, double Cb, double Cr) {
		int r = (int)(Y + 1.402 * Cr); 
		int g =(int)( Y - 0.3441*Cb - 0.7141*Cr);
		int b = (int) (Y + 1.772*Cb);
		int a[] = {r,g,b};
		return  a;
    	   }
    
    
    }

