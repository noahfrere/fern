package edu.utk.phys.fern;
// ---------------------------------------------------------------------------------------------------------------
//  Class ContourFrame to lay out main interface window
//  for isotope abundance contour display.  Implements ItemListener
//  to listen for Checkbox events.  This requires explicit definition
//  of the method itemStateChanged to process the Checkbox events.  Also
//  implements ComponentListener and WindowListener interfaces to help take
//  care of some specific problems generated by overriding the update
//  method in ContourPad.  Corresponding methods must also be defined.
//  These remarks are further explained in comments contained below.
// ----------------------------------------------------------------------------------------------------------------

import java.io.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import gov.sandia.postscript.PSGr1;


class ContourFrame extends Frame implements ItemListener,ComponentListener, WindowListener {

    Font titleFont = new java.awt.Font("SanSerif", Font.BOLD, 12);
    FontMetrics titleFontMetrics = getFontMetrics(titleFont);
    Font buttonFont = new java.awt.Font("SanSerif", Font.BOLD, 11);
    FontMetrics buttonFontMetrics = getFontMetrics(buttonFont);
    Font textFont = new java.awt.Font("SanSerif", Font.PLAIN, 12);
    FontMetrics textFontMetrics = getFontMetrics(textFont);

    static boolean [] includeReaction = new boolean [9];
    static boolean helpWindowOpen = false;

    // Following variables control how many contours, whether lin or log,
    // and the spacing of the contours.  The contours are mapped to the
    // interval 0-1.  The min and max must lie in this interval (and for
    // log scales neither can be zero).

    boolean logContour = StochasticElements.logContour;       // log (true) or lin (false) contours
    double minLogContour = StochasticElements.minLogContour;  // Minimum rel contour if log scale
    double maxLogContour = StochasticElements.maxLogContour;  // Max rel contour if log
    double minLinContour = StochasticElements.minLinContour;  // Min rel contour if lin 
    double maxLinContour = StochasticElements.maxLinContour;  // Max rel contour if lin

    int sleepZero = 1010;

    MyHelpFrame hf = new MyHelpFrame();

    Color panelBackColor = MyColors.gray204;
    Color panelForeColor = MyColors.gray51;

    Button animateButton;
    Label timeField;

    // Index (0=small, 1=medium, 2=large) for current box size and constants
    // setting the dimension of corresponding boxes

    int currentSizeIndex = 0;
    static final int SMALLBOXSIZE = 12; //17;
    static final int MEDBOXSIZE = 26; //29;
    static final int LARGEBOXSIZE = 33;

    static int numberContours = 11;
    double [] contourFraction = new double[numberContours];
    static double [] contourRange = new double[numberContours];
    static Color [] contourColor = new Color[numberContours];

    static ContourPad gp;
    static ScrollPane sp;
    Panel cboxPanel = new Panel();
    Panel loopPanel;

    final Checkbox loopcbox[] = new Checkbox[2];
    CheckboxGroup cbg = new CheckboxGroup();

    String ts;
    
    String popColorMap = StochasticElements.popColorMap;   // Color table for contour plots
    
    
    // --------------------------------------------------
    //  Public constructor
    // --------------------------------------------------

    public ContourFrame() {

        double maxValue = contourMax();

        // Determine contour levels, depending on whether linear scales
        // or log scales have been specified by the boolean logContour

        if (logContour) {            // if log contour scale

            double deltalog = 0.434448229*( Math.log(maxLogContour)
                - Math.log(minLogContour) ) / (double)numberContours;
            for (int i=0; i<numberContours; i++) {
                contourFraction[i] = Math.pow(10, 0.434448229*Math.log(minLogContour)+ deltalog*(i+1));
                contourRange[i] = contourFraction[i]*maxValue;               
            }

        } else {                     // if linear contour scale
            double deltalin = (maxLinContour - minLinContour)/(double)numberContours;
            for (int i=0; i<numberContours; i++) {
                contourFraction[i] = minLinContour + deltalin*(i+1);
                contourRange[i] = contourFraction[i]*maxValue;
            }
        }

        // Set contour colors using a MyColors object
        
        MyColors mc = new MyColors();

        for(int i=0; i<contourRange.length; i++) {
            contourColor[i] = mc.returnRGB(StochasticElements.popColorInvert,
                StochasticElements.popColorMap,  (double)(i*0.1) );   
            //contourColor[i] = returnRGB( (double)(i*0.1) );           // Old method no longer used
        }

        setLayout(new BorderLayout());

        // Create a graphics canvas of class ContourPad and attach
        // it as a scrollable child to a ScrollPane

        gp = new ContourPad(contourRange, contourColor);
        sp = new ScrollPane(ScrollPane.SCROLLBARS_AS_NEEDED);
        sp.add(gp);
        this.add("Center", sp);

        // Add a component listener to gp to detect when it moves
        // so that we can trigger a repaint.  Also add a WindowListener
        // to ContourFrame (this) to listen for when it is hidden
        // (iconized) and redisplayed.  In that case, repaint gp
        // also.  See the ComponentListener and WindowListener
        // required methods defined below.

        gp.addComponentListener(this);

        this.addWindowListener(this);

        Panel p = new Panel();
        p.setLayout(new GridBagLayout());
        p.setForeground(panelForeColor);
        p.setBackground(panelBackColor);

        // Define a GridBagConstraint object that we will reuse
        // several times in different GridBagLayouts

        GridBagConstraints gs = new GridBagConstraints();
        gs.weightx = 100;
        gs.weighty = 100;
        gs.gridx = 0;
        gs.gridy = 0;
        gs.ipadx = 0;
        gs.ipady = 0;
        gs.fill = GridBagConstraints.BOTH;
        gs.anchor = GridBagConstraints.NORTH;
        gs.insets = new Insets(10,6,0,6);

        LegendPad lpd = new LegendPad(contourRange, contourColor);
        lpd.setBackground(ShowIsotopes.isoBC);
        cboxPanel.add(lpd);
        cboxPanel.setBackground(new Color(190,190,190));
        p.add(cboxPanel, gs);

        // Panel to hold action buttons

        Panel goPanel = new Panel();
        goPanel.setLayout(new GridBagLayout());
        goPanel.setBackground(new Color (190,190,190));
        gs.insets = new Insets(0,6,7,6);
        gs.ipady = 2;
        gs.fill = GridBagConstraints.HORIZONTAL;

        // Create time display panel and add to goPanel

        Panel timePanel = new Panel();
        timePanel.setLayout(new GridLayout(2,1));
        timePanel.setFont(buttonFont);
        timePanel.setForeground(new Color(0,0,0));
        timeField = new Label( StochasticElements.gg.decimalPlace(6,
            StochasticElements.timeNow[gp.t] ), Label.CENTER );
        timeField.setBackground(new Color(230,230,230));
        timeField.setForeground(new Color(0,0,0));
        timeField.setFont(buttonFont);
        Label timeLabel1 = new Label("Seconds", Label.CENTER);
        timePanel.add(timeLabel1);
        timePanel.add(timeField);
        gs.gridy = 0;
        gs.insets = new Insets(2,6,10,6);
        goPanel.add(timePanel,gs);
        gs.insets = new Insets(0,6,7,6);

        // Create button to increase timestep number and add to goPanel

        Button plusTButton = new Button("Timestep >");
        plusTButton.setFont(buttonFont);
        gs.gridy = 1;
        goPanel.add(plusTButton, gs);

        // Create button to decrease timestep number and add to goPanel

        Button minusTButton = new Button("Timestep <");
        minusTButton.setFont(buttonFont);
        gs.gridy = 2;
        goPanel.add(minusTButton, gs);

        // Create button to initiate animation and add to panel

        animateButton = new Button("Animate");
        animateButton.setFont(buttonFont);
        gs.gridy = 3;
        goPanel.add(animateButton, gs);

        // Create panel with toggle radio buttons to turn looping on and off.
        // Make part of a CheckBoxGroup cbg to enforce exclusive behavior.
        // Add ItemListeners to the Checkboxes; their actions will be processed
        // in the method itemStateChanged.

        loopPanel = new Panel();
        loopPanel.setLayout(new GridBagLayout());
        loopPanel.setFont(buttonFont);
        loopcbox[0] = new Checkbox("No Loop");
        loopcbox[1] = new Checkbox("Loop");
        loopcbox[0].setCheckboxGroup(cbg);
        loopcbox[1].setCheckboxGroup(cbg);
        loopcbox[0].addItemListener(this);
        loopcbox[1].addItemListener(this);
        cbg.setSelectedCheckbox(loopcbox[0]);
        gs.insets = new Insets(0,6,0,6);
        gs.gridx = gs.gridy = 0;
        loopPanel.add(loopcbox[0], gs);
        gs.gridy = 1;
        loopPanel.add(loopcbox[1], gs);

        gs.insets = new Insets(0,6,7,6);
        gs.gridx = 0;
        gs.gridy = 4;
        goPanel.add(loopPanel, gs);

        // Create animation speed control panel and add to panel

        Panel speedPanel = new Panel();
        speedPanel.setLayout(new GridBagLayout());
        speedPanel.setFont(buttonFont);
        Label speedLabel1 = new Label("Speed", Label.CENTER);
        final Scrollbar sbs = new Scrollbar(0,950,0,500,1001);
        gp.sleepTime = sleepZero - sbs.getValue();
        sbs.setBackground(new Color(230,230,230));
        gs.gridx = 0;
        gs.gridy = 0;
        gs.insets = new Insets(0,0,0,0);
        speedPanel.add(sbs, gs);
        gs.gridy = 1;
        speedPanel.add(speedLabel1, gs);

        gs.insets = new Insets(0,6,2,6);
        gs.gridx = 0;
        gs.gridy = 5;
        goPanel.add(speedPanel, gs);

        // Add the goPanel to the Panel p

        gs.anchor = GridBagConstraints.NORTH;
        gs.gridy = 1;
        p.add(goPanel, gs);
        goPanel.setForeground(panelForeColor);

        // Add the Panel p to the main layout

        this.add("East",p);

        // Bottom panel with widgets

        Panel botPanel = new Panel();
        botPanel.setLayout(new GridBagLayout());
        botPanel.setBackground(panelBackColor);
        botPanel.setForeground(panelForeColor);
        botPanel.setFont(buttonFont);

        // Reuse GridBagConstraints gs

        gs.weightx = 100;
        gs.weighty = 100;
        gs.gridx = 0;
        gs.gridy = 0;
        gs.ipadx = 0;
        gs.ipady = 0;
        gs.fill = GridBagConstraints.NONE;
        gs.anchor = GridBagConstraints.WEST;
        gs.insets = new Insets(4,10,4,0);

        Panel botPanelA = new Panel();
        Label boxSizeLabel = new Label("Box Size",Label.LEFT);
        botPanelA.add(boxSizeLabel);

        final Choice boxSize = new Choice();
        boxSize.setFont(textFont);
        boxSize.addItem("Small");
        boxSize.addItem("Medium");
        boxSize.addItem("Large");
        boxSize.select(0);            // Set Small by default
        botPanelA.add(boxSize);
        gs.gridx = 0;
        botPanel.add(botPanelA, gs);

        Panel botPanelB = new Panel();
        Label zmaxLabel = new Label("Zmax",Label.LEFT);
        botPanelB.add(zmaxLabel);

        final TextField zmaxField = new TextField();
        zmaxField.setFont(textFont);
        zmaxField.setText(Integer.toString(IsotopePad.zmax));
        botPanelB.add(zmaxField);
        gs.insets = new Insets(4,0,4,0);
        gs.gridx = 1;
        botPanel.add(botPanelB, gs);

        Panel botPanelC = new Panel();
        Label nmaxLabel = new Label("Nmax",Label.LEFT);
        botPanelC.add(nmaxLabel);

        final TextField nmaxField = new TextField();
        nmaxField.setFont(textFont);
        nmaxField.setText(Integer.toString(IsotopePad.nmax));
        botPanelC.add(nmaxField);
        gs.gridx = 2;
        botPanel.add(botPanelC, gs);

        Button resetButton = new Button("Reset");
        resetButton.setFont(buttonFont);
        gs.gridx = 3;
        botPanel.add(resetButton, gs);

        final Button isoButton = new Button("Show Isotopes");
        isoButton.setFont(buttonFont);
        gs.gridx = 4;
        botPanel.add(isoButton, gs);

        final Button allButton = new Button("Select All");
        allButton.setFont(buttonFont);
        gs.insets = new Insets(4,0,4,120);
        gs.gridx = 5;
        botPanel.add(allButton, gs);

        this.add("South",botPanel);

        // Add blank left and top panels for spacing

        Panel leftPanel = new Panel();
        leftPanel.setBackground(panelBackColor);
        leftPanel.setSize(10,this.getSize().height);

        this.add("West",leftPanel);

        Panel topPanel = new Panel();
        topPanel.setBackground(panelBackColor);
        topPanel.setSize(this.getSize().width,10);

        this.add("North",topPanel);


        // Button action to increase the timestep by one unit.
        // Handle with an inner class.

        plusTButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae){

                if(gp.t >= gp.ts) {
					gp.t = 0;
                } else {
                    gp.t ++;
                }

                    ts = StochasticElements.gg.decimalPlace(6,StochasticElements.timeNow[gp.t]);
                    gp.timerString = ts;
                    gp.boxRepainter(gp.t);
                    timeField.setText(ts);
                }
            });


        // Button action to decrease the timestep by one unit.
        // Handle with an inner class.

        minusTButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae){

                if(gp.t == 0) {
                    gp.t = gp.ts;
                } else {
                    gp.t --;
                }
                ts = StochasticElements.gg.decimalPlace(6,StochasticElements.timeNow[gp.t]);
                gp.timerString = ts;
                gp.boxRepainter(gp.t);
                timeField.setText(ts);
            }
        });



      // animateButton actions.  Handle with an inner class.

      animateButton.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent ae){

              if(gp.animator == null) {
                  gp.animateIt = true;
                  gp.startThread();
                  animateButton.setLabel("Stop");
              } else {
                  gp.animateIt = false;
                  gp.loopFlag = false;
                  animateButton.setLabel("Animate");
              }
          }

      });


      // Animation speed scrollbar actions.  Handle with inner class.

      sbs.addAdjustmentListener(new AdjustmentListener() {
          public void adjustmentValueChanged(AdjustmentEvent e){

              gp.sleepTime = sleepZero - sbs.getValue();

          }
      });


      // Actions for Reset button.  Handle with an inner class

      resetButton.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent ae){

          int index = boxSize.getSelectedIndex();
          if(index == 0 && gp.showIsoLabels){
              String message = "Can't use small boxes if isotope";
              message += " labels are displayed.  Either turn off";
              message += " isotope labels, or set size to medium";
              message += " or large.";

              makeTheWarning(300,300,200,150,Color.black,
                             Color.lightGray, " Warning!",
                             message, false, ContourFrame.this );
              return;

          } else if (index == 0){
              gp.boxWidth = gp.boxHeight = SMALLBOXSIZE;   // small boxes
              currentSizeIndex = 0;
          } else if (index == 1){
              gp.boxWidth = gp.boxHeight = MEDBOXSIZE;     // medium boxes
              currentSizeIndex = 1;
          } else if (index == 2){
              gp.boxWidth = gp.boxHeight = LARGEBOXSIZE;   // large boxes
              currentSizeIndex = 2;
          }

          // Clear the existing plot from the Graphics buffer by
          // overwriting with the background color

          gp.ig.setColor(ShowIsotopes.isoBC);
          gp.ig.fillRect(0,0,gp.xmax + gp.xoffset,gp.ymax + gp.yoffset);

          String zstring = zmaxField.getText();
          String nstring = nmaxField.getText();
          gp.zmax = stringToInt(zstring);
          gp.nmax = stringToInt(nstring);
          gp.width = gp.boxWidth*(gp.nmax+1);
          gp.height = gp.boxHeight*(gp.zmax+1);
          gp.setSize(gp.width+2*gp.xoffset,gp.height+2*gp.yoffset);
          gp.xmax = gp.xoffset + gp.width;
          gp.ymax = gp.yoffset + gp.height;

          // Write the current plot to the Graphics buffer

          gp.drawMesh(gp.xoffset,gp.yoffset,gp.boxWidth,gp.boxHeight,gp.ig);

          // Force redisplay to toggle scrollbar state
          // on scrollPane viewport if it has changed with
          // reset of zmax and nmax. Requires call to a method of outer
          // class because we are presently in an inner
          // class and this.show() is not recognized here.
          // Instead, to get a reference to "this" for the
          // outer class from an inner class, use the construction
          // "outerClassName.this" instead of "this"

          ContourFrame.this.show();

          // Reset scrollbars (if displayed) so that Z=0, N=0 is in
          // the lower left corner. Not sure how to determine the
          // coordinate for vertical scrollbar to position it at the
          // bottom, so make it larger (5000) than the vertical dimension
          // of the segre chart would ever be.

          sp.setScrollPosition(0,5000);

          gp.repaint();

          }
      });  // -- end inner class for Reset button processing



      // Actions for isoButton button.  Handle with an inner class

      isoButton.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent ae){
              if(gp.showIsoLabels){
                  isoButton.setLabel("Show Isotopes");
                  gp.showIsoLabels = false;
              } else {
                  if(currentSizeIndex == 0){
                     String message = "Can't use small boxes if isotope";
                     message += " labels are displayed.  Either turn off";
                     message += " isotope labels, or set size to medium";
                     message += " or large.";
                     makeTheWarning(300,300,200,150,Color.black,
                               Color.lightGray, " Warning!",
                               message, false, ContourFrame.this);
                     return;
                  }
                  isoButton.setLabel("Hide Isotopes");
                  gp.showIsoLabels = true;
              }

              // Write the current plot to the offscreen Graphics buffer

              gp.drawMesh(gp.xoffset,gp.yoffset,gp.boxWidth,gp.boxHeight,gp.ig);
              gp.repaint();
          }
      });


      // Actions for allButton button.  Handle with an inner class

      allButton.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent ae){


          }
      });



      // Add window closing button (inner class)

      this.addWindowListener(new WindowAdapter() {
         public void windowClosing(WindowEvent e) {
            gp.animateIt=false;
            ContourFrame.this.hide();
            ContourFrame.this.dispose();
         }
      });


    }  /* End ContourFrame constructor method */



    // ---------------------------------------------------------------------------------------------------------
    //  Static method makePS invoked from MyFileDialogue instance to write
    //  postscript file of Segre plane.  gp is not recognized
    //  directly from that object, but by declaring the instances
    //  of SegreFrame and IsotopePad to be static, methods of gp
    //  can be invoked indirectly through this method.  Likewise
    //  for the method makeRepaint invoked from AbundanceData.
    // ---------------------------------------------------------------------------------------------------------

    static void makePS(String file) {
        gp.PSfile(file);
    }


    // ---------------------------------------------------------------------------------------------------
    //  Static method makeRepaint invoked from AbundanceData to force
    //  reset of box color when an initial abundance is saved.
    // ---------------------------------------------------------------------------------------------------

    //static void makeRepaint() {
       //ContourPad.updateAbundance = true;
    //}



    // --------------------------------------------------------------------------------------------------------
    //  Method itemStateChanged to act when state of Checkboxes changes.
    //  Requires that the class (ContourFrame) implement the
    //  ItemListener interface, which in turn requires that the
    //  method itemStateChanged be defined explicitly since
    //  ItemListener is abstract.
    // ---------------------------------------------------------------------------------------------------------

    public void itemStateChanged(ItemEvent check) {

        // Process the Checkboxes.  First get the components
        // of the panel loopPanel and store in a Component
        // array (Note: the method getComponents()
        // is inherited from the Container class by the
        // subclass Panel).

        Component [] components = loopPanel.getComponents();

        // Now process these components.  First cast each
        // Component to a Checkbox.  Then use the getState()
        // method of Checkbox to return boolean true if
        // checked and false otherwise, and act accordingly.
        // Since in this case the two checkboxes are exclusive
        // (only one can be true), we only need to process
        // one of them to know what to do.

        gp.loopFlag = ((Checkbox) components[1]).getState();

    }



    // ------------------------------------------------------------------------------------------
    //  Static method stringToDouble to convert a string to a double
    // ------------------------------------------------------------------------------------------

    static double stringToDouble (String s) {
        Double mydouble=Double.valueOf(s);      // String to Double (object)
        return mydouble.doubleValue();                // Return primitive double
    }



    // --------------------------------------------------------------------------------
    //  Static method stringToInt to convert a string to an int
    // --------------------------------------------------------------------------------

    static int stringToInt (String s) {
        Integer myInt=Integer.valueOf(s);      // String to Integer (object)
        return myInt.intValue();                     // Return primitive int
    }



    // ----------------------------------------------------------------------------------------------
    //  Method printThisFrame prints an entire Frame (Java 1.1 API).
    //  To print a Frame or class subclassed from Frame, place this
    //  method in its class description and invoke it to print.
    //  It MUST be in a Frame or subclass of Frame.
    //  It invokes a normal print dialogue,
    //  which permits standard printer setup choices. I have found
    //  that rescaling of the output in the print dialogue works
    //  properly on some printers but not on others.  Also, I tried
    //  printing to a PDF file through PDFwriter but that caused
    //  the program to freeze, requiring a hard reboot to recover.
    //  (You can use the PSfile method to output to a .ps file.)
    //
    //      Variables:
    //      xoff = horizontal offset from upper left corner
    //      yoff = vertical offset from upper left corner
    //      makeBorder = turn rectangular border on or off
    // ------------------------------------------------------------------------------------------------

    public void printThisFrame(int xoff, int yoff, boolean makeBorder) {

        java.util.Properties printprefs = new java.util.Properties();
        Toolkit toolkit = this.getToolkit();
        PrintJob job = toolkit.getPrintJob(this,"Java Print",printprefs);
        if (job == null) {return;}
        Graphics g = job.getGraphics();
        g.translate(xoff,yoff);                      // Offset from upper left corner
        Dimension size = this.getSize();
        if (makeBorder) {                           // Rectangular border
            g.drawRect(-1,-1,size.width+2,size.height+2);
        }
        g.setClip(0,0,size.width,size.height);
        this.printAll(g);
        g.dispose();
        job.end();

    }



    // ------------------------------------------------------------------------------------------------------------------
    //  The method makeTheWarning creates a modal warning window when invoked
    //  from within an object that subclasses Frame. The window is
    //  modally blocked (the window from which the warning window is
    //  launched is blocked from further input until the warning window
    //  is dismissed by the user).  Method arguments:
    //
    //      X = x-position of window on screen (relative upper left)
    //      Y = y-position of window on screen (relative upper left)
    //      width = width of window
    //      height = height of window
    //      fg = foreground (font) color
    //      bg = background color
    //      title = title string
    //      text = warning string text
    //      oneLine = display as one-line Label (true)
    //                or multiline TextArea (false)
    //      frame = A Frame that is the parent window modally
    //              blocked by the warning window.  If the parent
    //              class from which this method is invoked extends
    //              Frame, this argument can be just "this" (or
    //              "ParentClass.this" if invoked from an
    //              inner class event handler of ParentClass).
    //              Otherwise, it must be the name of an object derived from
    //              Frame that represents the window modally blocked.
    //
    //  If oneLine is true, you must make width large enough to display all
    //  text on one line.
    // ----------------------------------------------------------------------------------------------------------------------

    public void makeTheWarning (int X, int Y, int width, int height,
        Color fg, Color bg, String title, String text, boolean oneLine, Frame frame) {

        Font warnFont = new java.awt.Font("SanSerif", Font.BOLD, 12);
        FontMetrics warnFontMetrics = getFontMetrics(warnFont);

        // Create Dialog window with modal blocking set to true.
        // Make final so inner class below can access it.

        final Dialog mww = new Dialog(frame, title, true);
        mww.setLayout(new BorderLayout());
        mww.setSize(width,height);
        mww.setLocation(X,Y);

        // Use Label for 1-line warning

        if (oneLine) {
            Label hT = new Label(text,Label.CENTER);
            hT.setForeground(fg);
            hT.setBackground(bg);
            hT.setFont(warnFont);
            mww.add("Center", hT);

        // Use TextArea for multiline warning

        } else {
            TextArea hT = new TextArea("",height,width,
                          TextArea.SCROLLBARS_NONE);
            hT.setEditable(false);
            hT.setForeground(fg);
            hT.setBackground(bg);  // no effect once setEditable (false)?
            hT.setFont(warnFont);
            mww.add("Center", hT);
            hT.appendText(text);
        }

        mww.setTitle(title);

        // Add dismiss button

        Panel botPanel = new Panel();
        botPanel.setBackground(Color.lightGray);
        Label label1 = new Label();
        Label label2 = new Label();

        Button dismissButton = new Button("Dismiss");
        botPanel.add(label1);
        botPanel.add(dismissButton);
        botPanel.add(label2);

        // Add inner class event handler for Dismiss button.  This must be
        // added to the dismissButton before botPanel is added to mww.

        dismissButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae){
              mww.hide();
              mww.dispose();
            }
        });

        mww.add("South", botPanel);

        // Add window closing button (inner class)

        mww.addWindowListener(new WindowAdapter() {
           public void windowClosing(WindowEvent e) {
              mww.hide();
              mww.dispose();
           }
        });

        mww.show();    // Note that this show must come after all the above
                              // additions; otherwise they are not added before the
                              // window is displayed.
    }




    // -------------------------------------------------------------------------------------------------------
    //  Method returnRGB to return an RGB color for contour plotter as
    //  a function of the fraction x (lying between 0 and 1) of the max
    //  contour level.  This produces a color continuously varying with
    //  x that goes from deep blue (at x=0) through greens, yellows and
    //  oranges, and finally red (at x=1).  To draw, e.g., a continuous horizontal
    //  color scale, call this method from within a loop that draws on a
    //  graphics object a series of short vertical lines, with the x coordinate
    //  increasing by 1 pixel each iteration, passing the argument x divided
    //  by the interval length (to normalize to unit x interval) to returnRGB
    //  in order to set the drawing color before each line draw.
    //
    //  THIS METHOD NO LONGER USED; SUPERCEDED BY 
    //  MyColors.returnRGB().
    // ----------------------------------------------------------------------------------------------------------

    public Color returnRGB (double x) {
    
        boolean invertMap = false;

        if (popColorMap.compareTo("cardall") == 0){
            InterpolateColorTable ict = new InterpolateColorTable(invertMap,
                MyColors.cx, MyColors.cR, MyColors.cG, MyColors.cB);
            return ict.rgb(x);
        } else if (popColorMap.compareTo("guidry") == 0) {
            InterpolateColorTable ict = new InterpolateColorTable(invertMap,
                MyColors.gx, MyColors.gR, MyColors.gG, MyColors.gB);
            return ict.rgb(x); 
        } else if (popColorMap.compareTo("guidry2") == 0) {
            InterpolateColorTable ict = new InterpolateColorTable(invertMap,
                MyColors.ggx, MyColors.ggR, MyColors.ggG, MyColors.ggB);
            return ict.rgb(x); 
        } else if (popColorMap.compareTo("bluehot") == 0) {
            InterpolateColorTable ict = new InterpolateColorTable(invertMap,
                MyColors.bhx, MyColors.bhR, MyColors.bhG, MyColors.bhB);
            return ict.rgb(x); 
        } else if (popColorMap.compareTo("caleblack") == 0) {
            InterpolateColorTable ict = new InterpolateColorTable(invertMap,
                MyColors.cbx, MyColors.cbR, MyColors.cbG, MyColors.cbB);
            return ict.rgb(x); 
        } else if (popColorMap.compareTo("calewhite") == 0) {
            InterpolateColorTable ict = new InterpolateColorTable(invertMap,
                MyColors.cwx, MyColors.cwR, MyColors.cwG, MyColors.cwB);
            return ict.rgb(x); 
        } else if (popColorMap.compareTo("hot") == 0) {
            InterpolateColorTable ict = new InterpolateColorTable(invertMap,
                MyColors.hx, MyColors.hR, MyColors.hG, MyColors.hB);
            return ict.rgb(x); 
        } else {
            double x0R = 0.8;
            double x0G = 0.5;
            double x0B = 0.2;
            double aR = 0.5;
            double aG = 0.5;
            double aB = 0.3;
    
            int red = (int) (255*Math.exp( -(x-x0R)*(x-x0R)/aR/aR ));
            int green = (int) (255*Math.exp( -(x-x0G)*(x-x0G)/aG/aG ));
            int blue = (int) (255*Math.exp( -(x-x0B)*(x-x0B)/aB/aB ));
            return new Color(red,green,blue);
        }

    }



    // --------------------------------------------------------------------------------------
    //  Method to determine the max value for the contour scale.  
    // --------------------------------------------------------------------------------------

    public double contourMax() {

        double plotMax = 0;
        int zmax = IsotopePad.zmax;
        int nmax = IsotopePad.nmax;

        for(int z=1; z<=zmax; z++) {
            for(int n=SegreFrame.gp.minDripN[z];
                n<=Math.min(SegreFrame.gp.maxDripN[z],nmax); n++ ) {
                for (int j=0; j<StochasticElements.numdt; j++) {
                    double tryIt = StochasticElements.intPop[z][n][j];
                    if(!StochasticElements.plotY) tryIt *=((double)(z+n));
                    if(tryIt >= plotMax) {
                        plotMax = tryIt;
                    }
                }
            }
        }
        return plotMax;
    }


    // -----------------------------------------------------------------------------------------------
    //  The following 4 methods must be provided since we have
    //  implemented the ComponentListener interface.  All must be
    //  formally implemented, though we will not use all.  Use this
    //  to force repaint of the Canvas extension gp attached to the
    //  ScrollPane sp if the sp scrollbars move.  This is required
    //  because I have overridden the update method of gp to prevent
    //  flickering in the animation and the overridden method does
    //  not seem to always repaint the part of gp that is not in the
    //  sp viewport.  Also, part of gp can be blanked if the window
    //  is iconized or covered by another window.  In principle the
    //  methods below detecting component hidden or shown could be
    //  used to force a repaint in that case, but these methods do not
    //  seem to respond consistently to iconizing or covering the window,
    //  so are useless in this context.  If part of the window fails
    //  to repaint in that case, any motion of the scrollbars on the
    //  viewport, or clicking the Reset button will repaint.
    //  In these methods, to analyze
    //  the events insert a  System.out.println(e.paramString()); .
    //  To get the name of the component triggering the event, insert
    //  a  System.out.println("component="+e.getComponent().getName()); .
    //  By using these statements, we deduce that the instance gp has
    //  the component name "canvas0" and the instance cd of present
    //  class has the name "frame0".
    // ------------------------------------------------------------------------------------------------------

    public void componentHidden(ComponentEvent e) {
    }

    public void componentMoved(ComponentEvent e) {
        if( e.getComponent().getName().equals("canvas0") ) {
            gp.repaint();
        }
    }

    public void componentResized(ComponentEvent e) {
    }

    public void componentShown(ComponentEvent e) {
    }


    // --------------------------------------------------------------------------------------------------
    //  Methods required by the WindowListener interface.  As for the
    //  ComponentEvent methods above, these are an attempt to force
    //  repaint when window is covered or deiconified, because my
    //  overriden update method in instance gp of ContourPad, necessary
    //  to prevent flickering under Java 1.1 (no Swing), means that
    //  the instance gp inside ScrollPane is not always repainted
    //  fully if the window is covered or iconified/deiconified.  These
    //  help, but do not completely cure the problem.
    // ---------------------------------------------------------------------------------------------------

    public void windowActivated(WindowEvent e) {
        if( e.getWindow().getName().equals("frame0") ) {
            gp.repaint();
        }
    }

    public void windowClosed(WindowEvent e) {
    }

    public void windowClosing(WindowEvent e) {
    }

    public void windowDeactivated(WindowEvent e) {
    }

    public void windowDeiconified(WindowEvent e) {
        if( e.getWindow().getName().equals("frame0") ) {
            gp.repaint();
        }
    }

    public void windowIconified(WindowEvent e) {
        gp.repaint();
    }

    public void windowOpened(WindowEvent e) {
    }

}  /* End class ContourFrame */

