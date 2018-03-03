package samrock.viewer;

import static samrock.utils.Utils.createJPanel;
import static samrock.utils.Utils.getNothingfoundlabel;
import static samrock.utils.Utils.getUsedRamAmount;
import static samrock.utils.Utils.showHidePopup;
import static samrock.viewer.Actions.GOTO_END;
import static samrock.viewer.Actions.GOTO_START;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dialog.ModalityType;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import samrock.manga.chapter.Chapter;
import samrock.manga.chapter.ChapterSavePoint;
import samrock.utils.RH;

class MangaChapterStrip extends JLabel {
    private static Logger logger = LoggerFactory.getLogger(MangaChapterStrip.class);

	private static final long serialVersionUID = 5616442373554642829L;

	private final Color unreadCountForeground;
	private final Font dialogFont;
	private final Font detailsFont;
	private final Color detailsforeground;
	/**
	 * this is amount of extra vertical scroll allowed after image ends
	 */
	private final int extraEndScroll = 20;

	/**
	 * what user sees x * -1
	 */
	double x = 0;
	/**
	 * what use sees y * -1
	 */
	double y = 0;
	double scale = 1;
	/**
	 * as well as min scale
	 */
	private double unitScale = 0.2d;
	/**
	 * amount of pixel to move vertically in one scroll
	 */
	private int unitY;
	/**
	 * amount pixel to move horizontally in one scroll
	 */
	private int unitX; 

	private double defaultUnitYDivider = 4;
	private double defaultUnitXDivider = 4;
	/**
	 * unitY = screen_height/unitYDivider
	 */
	private double unitYDivider = defaultUnitYDivider;
	/**
	 * unitY = screen_height/unitYDivider
	 */
	private double unitXDivider = defaultUnitXDivider;
	private BufferedImage image;
	private String mangaNameLabel, 	chapterNameLabel, imageSizeLabel, unreadCountLabel;
	private String xLabel, 	yLabel, scaleLabel;
	private int imgW, imgH; //image width , height

	public void load(Chapter chapter, ChapterSavePoint savePoint, String mangaName, int unreadCount) {
		setChapterName(chapter.getName());
		setMangaName(mangaName);
		setUnreadCount(unreadCount);
		loadSavePoint(savePoint);

		/*#2 
		 * 
		 * this is something heavy stuff
		 * 
		 * a manga chaper image is saved in jpeg format, but when loaded
		 * it is raw and can take upto 
		 * 
		 * 65500(maximum height) * 1500 (maximum width(i hope)) * 4 (bytes/px) = 393 Mb
		 *  
		 */

		ImageReader reader = ImageIO.getImageReadersByFormatName("jpeg").next();
		ImageReadParam param = reader.getDefaultReadParam();
		if(chapter.chapterFileExists()){
			try(ImageInputStream iis = ImageIO.createImageInputStream(chapter.getGetChapterFilePath().toFile())) {
				reader.setInput(iis, true, true);
				image = reader.read(0, param);
				reader.dispose();
			} catch (IOException e) {
			    logger.error( "Failed to load image: \r\n"+chapter, e);
				image = null;
			}
		}
		else
			image = null;

		if(image != null){
			imgW = image.getWidth();
			imgH = image.getHeight();
			xyUnitsCalculated = false;
			imageSizeLabel = "Image Size: "+imgW+" X "+imgH;
		}
		else{
			imgW = 0;
			imgH = 0;
			setChapterName(chapter.getFileName()+" : "+chapter.getGetChapterFilePath());
		}
	}

	public MangaChapterStrip() {
		setDoubleBuffered(false);
		detailsFont = RH.getFont("mangaviewer.details.font");
		detailsforeground = RH.getColor("mangaviewer.details.forground");
		unreadCountForeground = RH.getColor("mangaViewer.unreadCount.Foreground");
		dialogFont = RH.getFont("mangaviewer.dialog.font");	}

	private void setMangaName(String mangaName) {this.mangaNameLabel = "Manga: ".concat(mangaName);}

	void setChapterName(String chpaterName) {this.chapterNameLabel = "Chapter: ".concat(chpaterName);}

	private void setUnreadCount(int unreadCount) {
		this.unreadCountLabel = unreadCount == 0 ? "" : "Unread Count: ".concat(String.valueOf(unreadCount));
	}

	private boolean xyUnitsCalculated = false;

	int width,height;
	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		Graphics2D g2 = (Graphics2D)g;

		if(image != null){
			g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
					RenderingHints.VALUE_INTERPOLATION_BICUBIC);

			if(!xyUnitsCalculated){
				width = getWidth();
				height = getHeight();

				unitX = (int) (width/unitXDivider);
				unitY = (int) (height/unitYDivider);

				xyUnitsCalculated = true;
			}

			if(- y < 0)
				y = 0;

			boolean reachedEnd = y + imgH + extraEndScroll*scale < height/scale;

			if(reachedEnd)
				y = - (imgH*scale - height + extraEndScroll*scale)/scale;

			double x1 = width/2 - imgW*scale/2;
			//from here
			if(x1 + x < -imgW*scale/2)
				x = -x1 - imgW*scale/2;

			if(x1+x+imgW*scale/2 > width)
				x = width - x1 - imgW*scale/2;

			//to here

			/*the above code decides on how much image can go out of the screen based on scale.
			 * if i replace above with the this code, the image will stop moving horizontally if corner of screen and image meets, 
			 *  
			 * 
			 * 	if(x1 + x < 0)
				 x = - x1;
				if(x1+x+imgW*scale > w)
					x = w - x1 - imgW*scale;
			 */

			AffineTransform at = AffineTransform.getTranslateInstance(x1+x,y*scale);
			at.scale(scale, scale);
			g2.drawRenderedImage(image, at);
			
			g2.setColor(detailsforeground);
			g2.setFont(detailsFont);
			FontMetrics fm = g2.getFontMetrics();
			int fmH = fm.getHeight();

			g2.drawString(mangaNameLabel, 10, fmH*1);
			g2.drawString(chapterNameLabel, 10, fmH*2);

			g2.drawString(imageSizeLabel, 10, fmH*4);
			g2.drawString(xLabel, 10, fmH*5);
			g2.drawString(yLabel, 10, fmH*6);
			g2.drawString(scaleLabel, 10, fmH*7);

			g2.setColor(unreadCountForeground);
			g2.drawString(unreadCountLabel, 10, fmH*9);

			g2.setColor(Color.white);
			g2.drawString(getRamUsedString(), 10, fmH*11);

			g2.setColor(Color.red);
			g2.drawString("Press H for Help", 10, getHeight() - 50);
			if(reachedEnd){
				int fontSize =  (int) (extraEndScroll*(scale*2));

				g2.setFont(new Font("Consolas", 1, fontSize));
				fm = g2.getFontMetrics();
				int endLabelX = (int) (x + x1 + imgW*scale/2 + fontSize/2);
				int endLabelY = (int) (height - fontSize);
				int stringW = fm.stringWidth("END");
				int stringH = fm.getHeight();

				g2.setColor(Color.black);
				g2.fillRoundRect((int)(endLabelX - stringW*3d/2), endLabelY - stringH, stringW*2, height - fontSize, fontSize/2, fontSize/2);
				g2.setColor(Color.red);
				g2.drawString("END", endLabelX - stringW,  endLabelY);
			}
		}
		else{
			g2.setColor(Color.red);
			g2.setFont(new Font("Lucida Handwriting", Font.PLAIN, getWidth()/20));
			FontMetrics mt = g.getFontMetrics();
			g2.drawString("Image Not Found", getHeight()/2 - mt.getHeight()/2, getWidth()/2 - mt.stringWidth("Image Not Found")/2);
			xyUnitsCalculated = false;
		}

		g2.dispose();
		g.dispose();
		image.flush();
	}

	private long currentRamUsed = 0L;
	private String ramUsedString = ""; 
	private String getRamUsedString() {
		long l2 = getUsedRamAmount();
		
		if(l2 == currentRamUsed)
			return ramUsedString;
		else
			return ramUsedString = "RAM Used ~ ".concat(String.valueOf(currentRamUsed = l2));
	}

	/**
	 * {@link #setx(double)} + {@link #sety(double)} + {@link #setScale(double)} combined with exception , it doesnt call repaint() 
	 * @param s
	 */
	private void loadSavePoint(ChapterSavePoint s){
		x = s == null ? 0 : s.x;
		y = s == null ? 0 : s.y;
		scale = s == null ? 1.0 : s.scale;

		int b = (int)scale;

		if(scale == b)
			scaleLabel = "zoom : ".concat(String.valueOf(b)).concat("X");
		else
			scaleLabel = "zoom : ".concat(String.valueOf(scale).replaceFirst("(\\d+\\.\\d{1,2})\\d*", "$1")).concat("X");

		xLabel = "x : ".concat(String.valueOf((int)this.x*-1));
		yLabel = "y : ".concat(String.valueOf((int)this.y*-1));
	}

	private void setScale(double scale) {
		if(scale > unitScale){
			this.scale = scale;

			int b = (int)scale;

			if(scale == b)
				scaleLabel = "zoom : ".concat(String.valueOf(b)).concat("X");
			else
				scaleLabel = "zoom : ".concat(String.valueOf(scale).replaceFirst("(\\d+\\.\\d{1,2})\\d*", "$1")).concat("X");

			repaint();

		}
	}
	void zoomIn(){setScale(scale + unitScale);}
	void zoomOut(){setScale(scale - unitScale);}
	void zoom(double value){setScale(value);}

	private void setx(double x){
		this.x = x;
		xLabel = "x : ".concat(String.valueOf((int)this.x*-1));
		repaint();
	}

	private void sety(double y){
		this.y = y;
		yLabel = "y : ".concat(String.valueOf((int)this.y*-1));
		repaint();
	}

	void scrollDown(){sety(y - unitY);}
	void scrollUp(){sety(y + unitY);}
	void scrollLeft(){setx(x + unitX);}
	void scrollRight(){setx(x - unitX);}

	public void doThis(Actions action) {
		if(image == null)
			return;

		if(action == GOTO_START || action == GOTO_END){
			y = action == GOTO_START ? 0 : -65500;
			x = 0;
			scale = 1d;
			repaint();
			return;
		}

		Supplier<JTextField> getJTextField = () -> {
			JTextField f = new JTextField();
			f.setFont(dialogFont);
			return f;
		};

		Function<String, JButton> getButton = text -> {
			JButton f = new JButton(text);
			f.setFont(dialogFont);
			return f;
		};

		JDialog d = new JDialog(null, ModalityType.APPLICATION_MODAL);
		d.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		d.getContentPane().setBackground(Color.BLACK);
		
		KeyStroke stroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, true);

		d.getRootPane().getActionMap().put("close_dialog", new AbstractAction() {
			private static final long serialVersionUID = 2768617611972791578L;

			@Override
			public void actionPerformed(ActionEvent e) {
				d.dispatchEvent(new WindowEvent(d, WindowEvent.WINDOW_CLOSING));
			}
		});
		
		d.getRootPane().getInputMap().put(stroke, "close_dialog");
		
		final JButton okButton = getButton.apply("Ok");
		
		stroke = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0, true);

		okButton.getActionMap().put("okButton", new AbstractAction() {
			private static final long serialVersionUID = 2768617611972791578L;

			@Override
			public void actionPerformed(ActionEvent e) {
				okButton.doClick();
			}
		});
		
		okButton.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(stroke, "okButton");
		
		JTextField askFocus = null;
		
		switch (action) {
		case OPEN_HELP_FILE:
		    JLabel jl ;
	        try(InputStream is = ClassLoader.getSystemResourceAsStream(RH.getString("mangaviewer.helpfile"));
	                InputStreamReader isr = new InputStreamReader(is);
	                BufferedReader rdr = new BufferedReader(isr)) {
	            String s = rdr.lines().collect(Collectors.joining("\n", "<html><pre>", "</pre></html>"));
	            
	            jl  = makeLabel(s);
	            jl.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 20));
	            jl.setOpaque(true);
	            jl.setBackground(Color.black);
	        } catch (IOException|NullPointerException e2) {
	            logger.error("Error to open helpfile", e2);
	            return;
	        }
			d.add(new JScrollPane(jl));
			break;
		case CHANGE_ZOOM:
			JPanel p = createJPanel(new GridLayout(3, 1, 5, 5));
			p.add(makeLabel("zoom (min value:"+(unitScale)+")"));
			JTextField f = getJTextField.get();
			p.add(f);
			p.add(okButton);
			askFocus = f;

			okButton.addActionListener(e -> {
				if(f.getText().trim().matches("(?:\\d+(?:\\.\\d*)?)|(?:\\d*\\.\\d+)")){
					double scale2 = Double.parseDouble(f.getText().trim());
					if(scale2 < unitScale)
						showHidePopup("input less that minimum", 1500);
					else {
						d.dispose();
						scale = scale2;
						repaint();
					}
				}
				else
					showHidePopup("Invalid Input", 1500);
			});

			p.setBorder(new EmptyBorder(10, 10, 10, 10));
			d.add(p);
			break;
		case GOTO:
			p = createJPanel(new GridLayout(6, 1, 6, 6));
			JLabel l =makeLabel("Enter Y (max value:"+imgH+")");
			p.add(l);
			JTextField yf = getJTextField.get();
			p.add(yf);
			askFocus = yf;

			p.add(makeLabel("<html>Or calculate Y by, n/m part of image height</html>"));

			JPanel p2 = createJPanel(new GridLayout(1, 4,5,5));
			l = makeLabel("n(th) part");
			l.setHorizontalTextPosition(JLabel.RIGHT);
			p2.add(l);
			JTextField nf = getJTextField.get();
			p2.add(nf);
			p2.setBorder(LineBorder.createGrayLineBorder());

			l = makeLabel("of m parts");
			l.setHorizontalTextPosition(JLabel.RIGHT);
			p2.add(l);
			JTextField mf = getJTextField.get();
			p2.add(mf);

			KeyListener keyListener = new KeyAdapter() {
				@Override
				public void keyReleased(KeyEvent e) {
					if(nf.getText().matches("\\d+") && mf.getText().matches("\\d+")){
						int n  = Integer.parseInt(nf.getText());
						int m  = Integer.parseInt(mf.getText());

						if(n < m)
							yf.setText(String.valueOf(n*(imgH/m)));
					}
				}
			};

			FocusAdapter focusAdapter = new FocusAdapter() {
				@Override
				public void focusLost(FocusEvent e) {

					if(nf.getText().matches("\\d+") && mf.getText().matches("\\d+")){
						int n  = Integer.parseInt(nf.getText());
						int m  = Integer.parseInt(mf.getText());
						if(n < m)
							yf.setText(String.valueOf(n*(imgH/m)));
					}
					else if(!nf.getText().isEmpty() && !mf.getText().isEmpty())
						showHidePopup("Only numeric +ve values are allowed", 1500);
				}
			};
			nf.addKeyListener(keyListener);
			nf.addFocusListener(focusAdapter);

			mf.addKeyListener(keyListener);
			mf.addFocusListener(focusAdapter);

			p.add(p2);

			p.add(makeLabel("of image height ("+imgH+")"));

			p.add(okButton);

			okButton.addActionListener(e -> {
				if(yf.getText().trim().isEmpty())
					showHidePopup("Y Field is empty", 1500);
				else if(!yf.getText().trim().matches("\\d+"))
					showHidePopup("Only numeric +ve values are allowed", 1500);
				else {
					x = 0;
					y = Integer.parseInt(yf.getText().trim())*-1; 
					repaint();
					d.dispose();
				}
			});

			p.setBorder(new EmptyBorder(10, 10, 10, 10));
			d.add(p);
			break;
		case CHANGE_SCROLL:
			p = createJPanel(new GridLayout(6, 2,10,5));
			p.add(makeLabel("Unit X"));
			JTextField xf = getJTextField.get();
			p.add(xf);
			p.add(makeLabel("Unit Y"));
			yf = getJTextField.get();
			p.add(yf);
			p.add(makeLabel("-------------"));
			p.add(makeLabel("-------------"));
			p.add(makeLabel("Unit X Divider"));
			JTextField xfd = getJTextField.get();
			p.add(xfd);
			askFocus = xfd;
			p.add(makeLabel("Unit Y Divider"));
			JTextField yfd = getJTextField.get();
			p.add(yfd);

			xf.setText(String.valueOf((int)(width/unitXDivider)));
			yf.setText(String.valueOf((int)(height/unitYDivider)));
			xfd.setText(String.valueOf(unitXDivider));
			yfd.setText(String.valueOf(unitYDivider));

			xf.setName("xf");
			yf.setName("yf");
			xfd.setName("xfd");
			yfd.setName("yfd");

			BiConsumer<JTextField, JTextField> consumer  = (originator, counterpart) -> {
				int max = originator.getName().contains("x") ? width : height;

				if(originator.getText().isEmpty()){
					counterpart.setText("");
					return;
				}
				if(originator.getName().endsWith("d") && originator.getText().matches("(?:\\d+(?:\\.\\d*)?)|(?:\\d*\\.\\d+)")){
					double a = Double.parseDouble(originator.getText());
					if(a >= max)
						originator.setText(String.valueOf(max));
					counterpart.setText(String.valueOf((int)(a >= max ? 1 : max/a)));
				}
				else if(originator.getName().endsWith("f") && originator.getText().matches("\\d+")){
					int a = Integer.parseInt(originator.getText());
					if(a >= max)
						originator.setText(String.valueOf(max));
					counterpart.setText(String.valueOf(a >= max ? 1 : max/a));
				}
				else
					showHidePopup(originator.getText()+" is Invalid value", 1000);
			};

			KeyListener keyl = new KeyAdapter() {
				@Override
				public void keyReleased(KeyEvent e) {
					switch (e.getComponent().getName()) {
					case "xf": consumer.accept(xf, xfd); break;
					case "yf": consumer.accept(yf, yfd); break;
					case "xfd": consumer.accept(xfd, xf); break;
					case "yfd": consumer.accept(yfd, yf); break;
					default:
						break;
					}
				}
			};

			xf.addKeyListener(keyl);
			yf.addKeyListener(keyl);
			xfd.addKeyListener(keyl);
			yfd.addKeyListener(keyl);

			JButton set1px = getButton.apply("Set 1px");
			JButton setdefaults = getButton.apply("Set Defaults");
			setdefaults.setMnemonic(KeyEvent.VK_D);
			set1px.setMnemonic(KeyEvent.VK_1);

			set1px.addActionListener(e -> {
				xf.setText("1");
				yf.setText("1");
				xfd.setText(String.valueOf(width));
				yfd.setText(String.valueOf(height));
			});

			setdefaults.addActionListener(e -> {
				xf.setText(String.valueOf((int)(width/defaultUnitXDivider)));
				yf.setText(String.valueOf((int)(height/defaultUnitYDivider)));
				xfd.setText(String.valueOf(defaultUnitXDivider));
				yfd.setText(String.valueOf(defaultUnitYDivider));
			});

			p.add(set1px);
			p.add(setdefaults);

			p.setBorder(new EmptyBorder(10, 10, 10, 10));

			okButton.addActionListener(e -> {
				if(xf.getText().matches("\\d+") && 
						yf.getText().matches("\\d+") &&
						yfd.getText().matches("(?:\\d+(?:\\.\\d*)?)|(?:\\d*\\.\\d+)") &&
						xfd.getText().matches("(?:\\d+(?:\\.\\d*)?)|(?:\\d*\\.\\d+)")
						){
					d.dispose();

					unitX = Integer.parseInt(xf.getText());
					unitY = Integer.parseInt(yf.getText());
					unitXDivider = Double.parseDouble(xfd.getText());
					unitYDivider = Double.parseDouble(yfd.getText());

				}
				else
					showHidePopup("Invalid values", 1000);
			});

			d.add(p);
			d.add(okButton, BorderLayout.SOUTH);	
			break;
		default:
			d.add(getNothingfoundlabel("failed to recognize code"+action));
			break;
		}
		
		d.pack();
		d.setLocationRelativeTo(null);
		if(askFocus != null)
		askFocus.requestFocus();
		d.setVisible(true);
	}

    private JLabel makeLabel(String text) {
        JLabel j = new JLabel(text, JLabel.LEFT);
        j.setForeground(Color.white);
        j.setFont(dialogFont);
        return j;
    }
}
