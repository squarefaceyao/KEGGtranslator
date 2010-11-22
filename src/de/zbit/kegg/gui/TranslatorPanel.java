/**
 *
 * @author wrzodek
 */
package de.zbit.kegg.gui;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.LinkedList;

import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileFilter;

import org.sbml.jsbml.SBMLDocument;
import org.sbml.tolatex.SBML2LaTeX;
import org.sbml.tolatex.gui.LaTeXExportDialog;
import org.sbml.tolatex.io.LaTeXOptionsIO;

import y.view.EditMode;
import y.view.Graph2D;
import y.view.Graph2DView;
import y.view.Graph2DViewMouseWheelZoomListener;
import de.zbit.gui.GUITools;
import de.zbit.gui.ProgressBarSwing;
import de.zbit.gui.VerticalLayout;
import de.zbit.io.SBFileFilter;
import de.zbit.kegg.Translator;
import de.zbit.kegg.gui.TranslatorUI.Action;
import de.zbit.kegg.io.AbstractKEGGtranslator;
import de.zbit.kegg.io.BatchKEGGtranslator;
import de.zbit.kegg.io.KEGGtranslator;
import de.zbit.util.AbstractProgressBar;
import de.zbit.util.StringUtil;
import de.zbit.util.prefs.SBPreferences;

/**
 * This should be used as a panel on a JTabbedPane.
 * It handles all the translating and visualizing, etc. of a KEGG pathway.
 * @author wrzodek
 */
public class TranslatorPanel extends JPanel {
  private static final long serialVersionUID = 6030311193210321410L;
  final File inputFile;
  final String outputFormat;
  boolean documentHasBeenSaved=false;
  
  /**
   * Result of translating {@link #inputFile} to {@link #outputFormat}.
   */
  Object document = null;
  /**
   * An action is fired to this listener, when the translation is done
   * or failed with an error.
   */
  ActionListener translationListener = null;
  
  /**
   * Create a new translator-panel and initiates the translation.
   * @param inputFile
   * @param outputFormat
   */
  public TranslatorPanel(final File inputFile, final String outputFormat, ActionListener translationResult) {
    super();
    setLayout(new BorderLayout());
    setOpaque(false);
    this.inputFile = inputFile;
    this.outputFormat = outputFormat;
    this.translationListener = translationResult;
    
    translate();
  }
  
  
  /**
   * Translates the {@link #inputFile} to {@link #outputFormat}.
   */
  public void translate() {
    final AbstractProgressBar pb = generateLoadingPanel(this, "Translating pathway...");
    final JComponent thiss = this;
    
    final SwingWorker<Object, Void> translator = new SwingWorker<Object, Void>() {
      @Override
      protected Object doInBackground() throws Exception {
        AbstractKEGGtranslator<?> translator = (AbstractKEGGtranslator<?>) BatchKEGGtranslator.getTranslator(outputFormat, Translator.getManager());
        translator.setProgressBar(pb);
        return translator.translate(inputFile);
      }
      protected void done() {
        removeAll();
        // Get the resulting document and check and handle eventual errors.
        try {
          document = get();
        } catch (Exception e) {
          e.printStackTrace();
          GUITools.showErrorMessage(null, e);
          fireActionEvent(new ActionEvent(thiss,JOptionPane.ERROR,TranslatorUI.Action.TRANSLATION_DONE.toString()));
          return;
        }
        
        // Change the tab to the corresponding content.
        if (isSBML()) {
          SBMLDocument doc = (SBMLDocument) document;
          
          // Create a new visualization of the model.
          try {
            add(new SBMLModelSplitPane(doc));
          } catch (Exception e) {
            e.printStackTrace();
            GUITools.showErrorMessage(null, e);
            fireActionEvent(new ActionEvent(thiss,JOptionPane.ERROR,TranslatorUI.Action.TRANSLATION_DONE.toString()));
            return;
          }
          
        } else if (isGraphML()) {
          // Create a new visualization of the model.
          Graph2DView pane = new Graph2DView((Graph2D) document);
          add(pane);
          
          pane.setSize(getSize());
          //ViewMode mode = new NavigationMode();
          //pane.addViewMode(mode);
          EditMode editMode = new EditMode();
          editMode.showNodeTips(true);
          pane.addViewMode(editMode);
          
          pane.getCanvasComponent().addMouseWheelListener(new Graph2DViewMouseWheelZoomListener());
          pane.fitContent(true);
        } 
        
        // Fire the listener
        validate();
        repaint();
        fireActionEvent(new ActionEvent(thiss,JOptionPane.OK_OPTION,TranslatorUI.Action.TRANSLATION_DONE.toString()));
        return;
      }

    };
    
    // Run the worker
    translator.execute();
  }
  

  /**
   * @param actionEvent
   */
  private void fireActionEvent(ActionEvent actionEvent) {
    if (translationListener!=null) {
      translationListener.actionPerformed(actionEvent);
    }
  }

  /**
   * Returns a string representation of the contained pathway.
   * @return
   */
  public String getTitle() {
    if (isSBML()) {
      SBMLDocument doc = (SBMLDocument) document;
      // Set nice title
      String title = doc.isSetModel() && doc.getModel().isSetId() ? doc.getModel().getId() : doc.toString();
      return title;
    } else {
      return inputFile.getName();
    }
  }
  
  
  /**
   * Create and display a temporary loading panel with the given message and a
   * progress bar.
   * @param parent - may be null. Else: all elements will be placed on this container
   * @return - the ProgressBar of the container.
   */
  private static AbstractProgressBar generateLoadingPanel(Container parent, String loadingText) {
    Dimension panelSize = new Dimension(400, 75);
    
    // Create the panel
    Container panel = new JPanel(new VerticalLayout());
    panel.setPreferredSize(panelSize);
    
    // Create the label and progressBar
    JLabel jl = new JLabel((loadingText!=null && loadingText.length()>0)?loadingText:"Please wait...");
    //Font font = new java.awt.Font("Tahoma", Font.PLAIN, 12);
    //jl.setFont(font);
    
    JProgressBar prog = new JProgressBar();
    prog.setPreferredSize(new Dimension(panelSize.width - 20,
      panelSize.height / 4));
    panel.add(jl, BorderLayout.NORTH);
    panel.add(prog, BorderLayout.CENTER);
    
    if (panel instanceof JComponent) {
      GUITools.setOpaqueForAllElements((JComponent) panel, false);
    }
    
    if (parent!=null) {
      parent.add(panel);
    } else {
      // Display the panel in an jFrame
      JDialog f = new JDialog();
      f.setTitle(KEGGtranslator.appName);
      f.setSize(panel.getPreferredSize());
      f.setContentPane(panel);
      f.setPreferredSize(panel.getPreferredSize());
      f.setLocationRelativeTo(null);
      f.setVisible(true);
      f.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
    }
    
    return new ProgressBarSwing(prog);
  }
  
  
  /**
   * @return true if and only if this panel contains an sbml document.
   */
  public boolean isSBML() {
    return (document!=null && document instanceof SBMLDocument);
  }
  
  /**
   * @return true if and only if this panel contains an Graph2D document.
   */
  public boolean isGraphML() {
    return (document!=null && document instanceof Graph2D);
  }
  
  public void saveToFile() {
    LinkedList<FileFilter> ff = new LinkedList<FileFilter>();
    
    if (isSBML()) {
      ff.add(SBFileFilter.SBML_FILE_FILTER);
      ff.add(SBFileFilter.TeX_FILE_FILTER);
      ff.add(SBFileFilter.PDF_FILE_FILTER);
    } else if (isGraphML()){
      ff.add(SBFileFilter.GRAPHML_FILE_FILTER);
      ff.add(SBFileFilter.GML_FILE_FILTER);
      ff.add(SBFileFilter.JPEG_FILE_FILTER);        
      ff.add(SBFileFilter.GIF_FILE_FILTER);
      ff.add(SBFileFilter.YGF_FILE_FILTER);
      ff.add(SBFileFilter.TGF_FILE_FILTER);
      for (int i=0; i<ff.size(); i++) {
        if (ff.get(i).toString().toLowerCase().startsWith(this.outputFormat.toLowerCase())) {
          ff.addFirst(ff.remove(i));
          break;
        }
      }
    } else {
      return;
    }
    
    // We also need to know the selected file filter!
    //File file = GUITools.saveFileDialog(this, TranslatorUI.saveDir, false, false, true,
      //JFileChooser.FILES_ONLY, ff.toArray(new FileFilter[0]));
    JFileChooser fc = GUITools.createJFileChooser(TranslatorUI.saveDir, false,
      false, JFileChooser.FILES_ONLY, ff.toArray(new FileFilter[0]));
    if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
    
    // Check file
    File f = fc.getSelectedFile();
    boolean showOverride = f.exists();
    if (!f.exists()) try {
      f.createNewFile();
    } catch (IOException e) {
      GUITools.showErrorMessage(this, e);
      e.printStackTrace();
      return;
    }
    if (!f.canWrite() || f.isDirectory() || (showOverride && !GUITools.overwriteExistingFile(this, f))) {
      JOptionPane.showMessageDialog(this, StringUtil.toHTML(
        "Cannot write to file " + f.getAbsolutePath() + ".", 60),
        "No writing access", JOptionPane.WARNING_MESSAGE);
    }
    
    System.out.println(f);
    System.out.println(fc.getFileFilter().getDescription());
    // TODO: Continue saving work..
    
    saveToFile(f);
  }
  public void saveToFile(File file) {
    // TODO: Implement.
    if (file != null) {
      TranslatorUI.saveDir = file.getParent();
      if (SBFileFilter.isTeXFile(file) || SBFileFilter.isPDFFile(file)) {
        writeLaTeXReport(file);
      } else {
        
      }
    }
    
    documentHasBeenSaved = true;
  }
  
  /**
   * @return true, if the document has been saved.
   */
  public boolean isSaved() {
    return (document==null || documentHasBeenSaved);
  }


  /**
   * Enabled and disables item in the menu bar, based on the content of this panel.
   * @param menuBar
   */
  public void updateButtons(JMenuBar menuBar) {
    if (isSBML()) {
      GUITools.setEnabled(true, menuBar, Action.SAVE_FILE, Action.TO_LATEX, Action.CLOSE_MODEL);
    } else if (isGraphML()) {
      GUITools.setEnabled(true, menuBar, Action.SAVE_FILE, Action.CLOSE_MODEL);
      GUITools.setEnabled(false, menuBar,Action.TO_LATEX);
    } else {
      // E.g. when translation still in progress
      GUITools.setEnabled(false, menuBar, Action.SAVE_FILE, Action.TO_LATEX, Action.CLOSE_MODEL);
    }
  }
  
  /**
   * @param targetFile - can be null.
   */
  public void writeLaTeXReport(File targetFile) {
    if (document==null) return;
    if (!isSBML()) {
      GUITools.showMessage("This option is only available for SBML documents.", KEGGtranslator.appName);
      return;
    }
    
    SBMLDocument doc = (SBMLDocument) document;
    if ((doc != null) && LaTeXExportDialog.showDialog(null, doc, targetFile)) {
      if (targetFile == null) {
        SBPreferences prefsIO = SBPreferences.getPreferencesFor(LaTeXOptionsIO.class);
        targetFile = new File(prefsIO.get(LaTeXOptionsIO.REPORT_OUTPUT_FILE));
      }
      try {
        SBML2LaTeX.convert(doc, targetFile, true);
      } catch (Exception exc) {
        GUITools.showErrorMessage(null, exc);
      }
    }
  }
  
  
}