package edu.utk.phys.fern;
// ------------------------------------------------------------------------------------------------------
// NOTE:  This class has generally been superceded by the method
//        makeTheWarning included in various classes because
//        makeTheWarning generates a modal dialog (one that blocks
//        further action in the main window).  The warning window
//        generated by the present class isn't modal.
//
//  Class MyWarning to create generic warning window.
//  Constructor arguments:
//      X = x-position of window on screen (relative upper left)
//      Y = y-position of window on screen (relative upper left)
//      width = width of window
//      height = height of window
//      fg = foreground (font) color
//      bg = background color
//      title = title string
//      text = warning string text
//      oneLine = display as one-line Label (true) or
//               multiline TextArea (false)
//  If oneLine is true, you must make width large
//  enough to display all text on one line.
// -----------------------------------------------------------------------------------------------------------


import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Frame;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextArea;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;


class MyWarning extends Frame {

    Font warnFont = new java.awt.Font("SanSerif", Font.BOLD, 12);
    FontMetrics warnFontMetrics = getFontMetrics(warnFont);
    
    // ------------------------------------------------------------
    //  Constructor
    // ------------------------------------------------------------
    
    public MyWarning(int X, int Y, int width, int height,
        Color fg, Color bg, String title, String text, boolean oneLine) {
    
        this.setLayout(new BorderLayout());
        this.setSize(width,height);
        this.setLocation(X,Y);
    
        // Use Label for 1-line warning
    
        if (oneLine) {
            Label hT = new Label(text,Label.CENTER);
            hT.setForeground(fg);
            hT.setBackground(bg);
            hT.setFont(warnFont);
            this.add("Center", hT);
    
        // Use TextArea for multiline warning
    
        } else {
            TextArea hT = new TextArea("",height,width,
                            TextArea.SCROLLBARS_NONE);
            hT.setEditable(false);
            hT.setForeground(fg);
            hT.setBackground(bg);  // no effect once setEditable (false)?
            hT.setFont(warnFont);
            this.add("Center", hT);
            hT.appendText(text);
        }
    
        this.setTitle(title);
    
        // Add dismiss button
    
        Panel botPanel = new Panel();
        botPanel.setBackground(Color.lightGray);
        Label label1 = new Label();
        Label label2 = new Label();
    
        Button dismissButton = new Button("Dismiss");
        botPanel.add(label1);
        botPanel.add(dismissButton);
        botPanel.add(label2);
    
        this.add("South", botPanel);
    
        // Add inner class event handler for Dismiss button
    
        dismissButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae){
                hide();
                dispose();
            }
        });
    
    
        // Add window closing button (inner class)
    
        this.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                hide();
                dispose();
            }
        });
    }

}  /* End class MyWarning */

