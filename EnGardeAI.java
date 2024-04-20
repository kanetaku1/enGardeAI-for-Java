package enGardeAI;

import engarde.gui.Constants;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
 
 /**
  *
  * @author TAKURO
  */
 public class EnGardeAI {

     public static void main(String[] args) {
         //Nomal Mode
         Constants.setFiles();
         //
         //Debug for ktajima
 //        File dataDir = new File("C:\\Users\\ktajima\\OneDrive - 独立行政法人 国立高等専門学校機構\\NetBeansProjects\\novaluna\\img");
 //        Constants.setFiles(dataDir);
         //
         
         try {
             UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
         } catch (ClassNotFoundException ex) {
             Logger.getLogger(EnGardeAI.class.getName()).log(Level.SEVERE, null, ex);
         } catch (InstantiationException ex) {
             Logger.getLogger(EnGardeAI.class.getName()).log(Level.SEVERE, null, ex);
         } catch (IllegalAccessException ex) {
             Logger.getLogger(EnGardeAI.class.getName()).log(Level.SEVERE, null, ex);
         } catch (UnsupportedLookAndFeelException ex) {
             Logger.getLogger(EnGardeAI.class.getName()).log(Level.SEVERE, null, ex);
         }
         mainFrameAI frame = new mainFrameAI();
         frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
         frame.setVisible(true);
     }
 }
 