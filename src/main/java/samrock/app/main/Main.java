package samrock.app.main;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.MissingResourceException;

import javax.swing.JOptionPane;

import samrock.gui.SamRock;
import samrock.manga.maneger.MangaManeger;
import samrock.utils.Utils;

public class Main {
	public static final double VERSION = 7.09;

	public static void main(String[] args) {
		File file = new File("running");
		if(file.exists()){
			JOptionPane.showMessageDialog(null, "Already Running", "One app is allowed", JOptionPane.WARNING_MESSAGE);
			System.exit(0);
		}
		else{
			try {
				file.createNewFile();
				file.deleteOnExit();
			} catch (IOException e) {	
				JOptionPane.showMessageDialog(null, e, "Error", JOptionPane.WARNING_MESSAGE);
				System.exit(0);
			}
		}
		try {
			Utils.load();
			MangaManeger.createInstance();
			SamRock sam = new SamRock(VERSION);
			sam.setVisible(true);
			while(!sam.isShowing()){}
		} catch (ClassNotFoundException | MissingResourceException | SQLException | InstantiationException | IllegalAccessException | IOException e) {
			Utils.openErrorDialoag(null, "Error Caught in MainMethod, App Will close",Main.class,45/*{LINE_NUMBER}*/, e);
			System.exit(0);
		}
	}
}
