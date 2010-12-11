/**
 * 
 */
package de.zbit.kegg.gui;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.prefs.BackingStoreException;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JTabbedPane;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.sbml.tolatex.gui.LaTeXExportDialog;

import de.zbit.gui.ActionCommand;
import de.zbit.gui.BaseFrame;
import de.zbit.gui.FileDropHandler;
import de.zbit.gui.GUIOptions;
import de.zbit.gui.GUITools;
import de.zbit.gui.ImageTools;
import de.zbit.gui.JColumnChooser;
import de.zbit.gui.prefs.FileHistory;
import de.zbit.gui.prefs.FileSelector;
import de.zbit.gui.prefs.PreferencesPanel;
import de.zbit.kegg.Translator;
import de.zbit.kegg.TranslatorOptions;
import de.zbit.kegg.io.KEGGtranslator;
import de.zbit.kegg.io.KEGGtranslatorHistory;
import de.zbit.util.StringUtil;
import de.zbit.util.prefs.KeyProvider;
import de.zbit.util.prefs.SBPreferences;
import de.zbit.util.prefs.SBProperties;

/**
 * @author Andreas Dr&auml;ger
 * @author Clemens Wrzodek
 * @date 2010-11-12
 */
public class TranslatorUI extends BaseFrame implements ActionListener,
		KeyListener, ItemListener {

	/**
	 * 
	 * @author Andreas Dr&auml;ger
	 * @date 2010-11-12
	 */
	public static enum Action implements ActionCommand {
		/**
		 * {@link Action} for LaTeX export.
		 */
		TO_LATEX,
		/**
		 * Invisible {@link Action} that should be performed, whenever an
		 * translation is done.
		 */
		TRANSLATION_DONE,
		/**
		 * Invisible {@link Action} that should be performed, whenever a file
		 * has been droppen on this panel.
		 */
		FILE_DROPPED;

		/*
		 * (non-Javadoc)
		 * 
		 * @see de.zbit.gui.ActionCommand#getName()
		 */
		public String getName() {
			switch (this) {
			case TO_LATEX:
				return "Export to LaTeX";
			default:
				return StringUtil.firstLetterUpperCase(toString().toLowerCase()
						.replace('_', ' '));
			}
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see de.zbit.gui.ActionCommand#getToolTip()
		 */
		public String getToolTip() {
			switch (this) {
			case TO_LATEX:
				return "Converts the currently opened model to a LaTeX report file.";
			default:
				return "Unknown";
			}
		}
	}

	/**
	 * Generated serial version identifier.
	 */
	private static final long serialVersionUID = 6631262606716052915L;

	static {
		ImageTools.initImages(LaTeXExportDialog.class.getResource("img"));
		ImageTools.initImages(TranslatorUI.class.getResource("img"));
	}

	/**
	 * Default directory path's for saving and opening files. Only init them
	 * once. Other classes should use these variables.
	 */
	public static String openDir, saveDir;
	/**
	 * This is where we place all the converted models.
	 */
	private JTabbedPane tabbedPane;
	/**
	 * prefs is holding all project specific preferences
	 */
	private SBPreferences prefs;

	/**
	 * 
	 */
	public TranslatorUI() {
		super();
		// init preferences
		prefs = SBPreferences.getPreferencesFor(TranslatorOptions.class);
		File file = new File(prefs.get(TranslatorOptions.INPUT));
		openDir = file.isDirectory() ? file.getAbsolutePath() : file
				.getParent();
		file = new File(prefs.get(TranslatorOptions.OUTPUT));
		saveDir = file.isDirectory() ? file.getAbsolutePath() : file
				.getParent();
		// Make this panel responsive to drag'n drop events.
		FileDropHandler dragNdrop = new FileDropHandler(this);
		this.setTransferHandler(dragNdrop);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.zbit.gui.BaseFrame#createJToolBar()
	 */
	protected JToolBar createJToolBar() {
		// final JPanel r = new JPanel(new VerticalLayout());
		final JToolBar r = new JToolBar("Translate new file",
				JToolBar.HORIZONTAL);

		r.add(PreferencesPanel.getJComponentForOption(TranslatorOptions.INPUT,
				prefs, this));
		// r.add(new JSeparator(JSeparator.VERTICAL));
		r.add(PreferencesPanel.getJComponentForOption(TranslatorOptions.FORMAT,
				prefs, this));

		// Button and action
		JButton ok = new JButton("Translate now!", UIManager
				.getIcon("ICON_GEAR_16"));
		ok.setToolTipText(StringUtil.toHTML(
								"Starts the conversion of the input file to the selected output format and displays the result on this workbench.",
								GUITools.TOOLTIP_LINE_LENGTH));
		ok.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				// Get selected file and format
				File inFile = getInputFile(r);
				String format = getOutputFileFormat(r);

				// Translate
				createNewTab(inFile, format);
			}
		});
		r.add(ok);

		GUITools.setOpaqueForAllElements(r, false);
		return r;
	}

	/**
	 * Searches for any JComponent with
	 * "TranslatorOptions.FORMAT.getOptionName()" on it and returns the selected
	 * format. Use it e.g. with {@link #translateToolBar}.
	 * 
	 * @param r
	 * @return String - format.
	 */
	private String getOutputFileFormat(JComponent r) {
		String format = null;
		for (Component c : r.getComponents()) {
			if (c.getName() == null) {
				continue;
			} else if (c.getName().equals(
					TranslatorOptions.FORMAT.getOptionName())
					&& (JColumnChooser.class.isAssignableFrom(c.getClass()))) {
				format = ((JColumnChooser) c).getSelectedItem().toString();
				break;
			}
		}
		return format;
	}

	/**
	 * Searches for any JComponent with
	 * "TranslatorOptions.INPUT.getOptionName()" on it and returns the selected
	 * file. Use it e.g. with {@link #translateToolBar}.
	 * 
	 * @param r
	 * @return File - input file.
	 */
	private File getInputFile(JComponent r) {
		File inFile = null;
		for (Component c : r.getComponents()) {
			if (c.getName() == null) {
				continue;
			} else if (c.getName().equals(
					TranslatorOptions.INPUT.getOptionName())
					&& (FileSelector.class.isAssignableFrom(c.getClass()))) {
				try {
					inFile = ((FileSelector) c).getSelectedFile();
				} catch (IOException e1) {
					GUITools.showErrorMessage(r, e1);
					e1.printStackTrace();
				}
			}
		}
		return inFile;
	}

	/**
	 * Translate and create a new tab.
	 * 
	 * @param inFile
	 * @param format
	 */
	private void createNewTab(File inFile, String format) {
		// Check input
		if (!TranslatorOptions.INPUT.getRange().isInRange(inFile)) {
			JOptionPane.showMessageDialog(this, '\'' + inFile.getName()
					+ "' is no valid input file.",
					KEGGtranslator.APPLICATION_NAME,
					JOptionPane.WARNING_MESSAGE);
		} else if (!TranslatorOptions.FORMAT.getRange().isInRange(format)) {
			JOptionPane.showMessageDialog(this, '\'' + format
					+ "' is no valid output format.",
					KEGGtranslator.APPLICATION_NAME,
					JOptionPane.WARNING_MESSAGE);
		} else {
			// Tanslate and add tab.
			try {
				openDir = inFile.getParent();
				tabbedPane.addTab(inFile.getName(), new TranslatorPanel(inFile,
						format, this));
				// tabbedPane.setSelectedComponent(tb);
			} catch (Exception e1) {
				GUITools.showErrorMessage(this, e1);
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent e) {
		try {
			Action action = Action.valueOf(e.getActionCommand());
			switch (action) {
			case TRANSLATION_DONE:
				TranslatorPanel source = (TranslatorPanel) e.getSource();
				if (e.getID() != JOptionPane.OK_OPTION) {
					// If translation failed, remove the tab. The error
					// message has already been issued by the translator.
					tabbedPane.removeTabAt(tabbedPane.indexOfComponent(source));
				} else {
					tabbedPane.setTitleAt(tabbedPane.indexOfComponent(source),
							source.getTitle());
				}
				updateButtons();
				break;
			case FILE_DROPPED:
				String format = getOutputFileFormat(toolBar);
				if ((format == null) || (format.length() < 1)) {
					break;
				}
				createNewTab(((File) e.getSource()), format);
				break;
			case TO_LATEX:
				writeLaTeXReport();
				break;
			default:
				System.out.println(action);
				break;
			}
		} catch (Throwable exc) {
			GUITools.showErrorMessage(this, exc);
		}
	}

	/**
	 * @param object
	 */
	private void writeLaTeXReport() {
		TranslatorPanel o = getCurrentlySelectedPanel();
		if (o != null) {
			o.writeLaTeXReport(null);
		}
	}

	/**
	 * Closes the tab at the specified index.
	 * 
	 * @param index
	 * @return true, if the tab has been closed.
	 */
	private boolean closeTab(int index) {
		if (index >= tabbedPane.getTabCount())
			return false;
		Component comp = tabbedPane.getComponentAt(index);
		String title = tabbedPane.getTitleAt(index);
		if (title == null || title.length() < 1) {
			title = "the currently selected document";
		}

		// Check if document already has been saved
		if ((comp instanceof TranslatorPanel)
				&& !((TranslatorPanel) comp).isSaved()) {
			if ((JOptionPane.YES_OPTION != JOptionPane.showConfirmDialog(this,
					StringUtil.toHTML(String.format(
							"Do you really want to close %s without saving?",
							title), 60), "Close selected document",
					JOptionPane.YES_NO_OPTION))) {
				return false;
			}
		}

		// Close the document.
		tabbedPane.removeTabAt(index);
		updateButtons();
		return true;
	}

	/*
	 * (non-Javadoc)
	 * @see de.zbit.gui.BaseFrame#openFile(java.io.File[])
	 */
	public File[] openFile(File... files) {
		// Ask input file
		if ((files == null) || (files.length < 1)) {
			files = GUITools.openFileDialog(this, openDir, false, true,
				JFileChooser.FILES_ONLY, new FileFilterKGML());
		}
		if ((files == null) || (files.length < 1)) {
			return files;
		}

		// Ask output format
		JColumnChooser outputFormat = (JColumnChooser) PreferencesPanel
				.getJComponentForOption(TranslatorOptions.FORMAT, null, null);
		outputFormat.setTitle("Please select the output format");
		JOptionPane.showMessageDialog(this, outputFormat,
				KEGGtranslator.APPLICATION_NAME, JOptionPane.QUESTION_MESSAGE);
		String format = ((JColumnChooser) outputFormat).getSelectedItem()
				.toString();

		// Translate
		for (File f : files) {
			createNewTab(f, format);
		}
		return files;
	}

	/**
	 * Enables and disables buttons in the menu, depending on the current tabbed
	 * pane content.
	 */
	private void updateButtons() {
		GUITools.setEnabled(false, getJMenuBar(), BaseAction.FILE_SAVE,
				Action.TO_LATEX, BaseAction.FILE_CLOSE);
		TranslatorPanel o = getCurrentlySelectedPanel();
		if (o != null) {
			o.updateButtons(getJMenuBar());
		}
	}

	/**
	 * @return the currently selected TranslatorPanel from the
	 *         {@link #tabbedPane}, or null if either no or no valid selection
	 *         exists.
	 */
	private TranslatorPanel getCurrentlySelectedPanel() {
		if ((tabbedPane == null) || (tabbedPane.getSelectedIndex() < 0)) {
			return null;
		}
		Object o = ((JTabbedPane) tabbedPane).getSelectedComponent();
		if ((o == null) || !(o instanceof TranslatorPanel)) {
			return null;
		}
		return ((TranslatorPanel) o);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.zbit.gui.BaseFrame#saveFile()
	 */
	public void saveFile() {
		TranslatorPanel o = getCurrentlySelectedPanel();
		if (o != null) {
			o.saveToFile();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * java.awt.event.WindowListener#windowActivated(java.awt.event.WindowEvent)
	 */
	public void windowActivated(WindowEvent we) {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.awt.event.KeyListener#keyPressed(java.awt.event.KeyEvent)
	 */
	public void keyPressed(KeyEvent e) {
		// Preferences for the "input file"
		PreferencesPanel.setProperty(prefs, e.getSource());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.awt.event.KeyListener#keyReleased(java.awt.event.KeyEvent)
	 */
	public void keyReleased(KeyEvent e) {
		// Preferences for the "input file"
		PreferencesPanel.setProperty(prefs, e.getSource());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.awt.event.KeyListener#keyTyped(java.awt.event.KeyEvent)
	 */
	public void keyTyped(KeyEvent e) {
		// Preferences for the "input file"
		PreferencesPanel.setProperty(prefs, e.getSource());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * java.awt.event.ItemListener#itemStateChanged(java.awt.event.ItemEvent)
	 */
	public void itemStateChanged(ItemEvent e) {
		// Preferences for the "output format"
		PreferencesPanel.setProperty(prefs, e.getSource());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.zbit.gui.BaseFrame#additionalFileMenuItems()
	 */
	protected JMenuItem[] additionalFileMenuItems() {
		return new JMenuItem[] { GUITools.createJMenuItem(this,
				Action.TO_LATEX, UIManager.getIcon("ICON_LATEX_16"), KeyStroke
						.getKeyStroke('E', InputEvent.CTRL_DOWN_MASK), 'E',
				false) };
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.zbit.gui.BaseFrame#closeFile()
	 */
	public boolean closeFile() {
		if (tabbedPane.getSelectedIndex() < 0) {
			return false;
		}
		return closeTab(tabbedPane.getSelectedIndex());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.zbit.gui.BaseFrame#createMainComponent()
	 */
	protected Component createMainComponent() {
		tabbedPane = new JTabbedPane();
		// Change active buttons, based on selection.
		tabbedPane.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				updateButtons();
			}
		});
		return tabbedPane;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.zbit.gui.BaseFrame#exit()
	 */
	public void exit() {
		// Close all tab. If user want's to save a tab first, cancel the closing
		// process.
		while (tabbedPane.getTabCount() > 0) {
			if (!closeTab(0)) {
				return;
			}
		}

		// Close the app and save caches.
		setVisible(false);
		try {
			Translator.saveCache();

			SBProperties props = new SBProperties();
			props.put(GUIOptions.OPEN_DIR, openDir);
			props.put(GUIOptions.SAVE_DIR, saveDir);
			SBPreferences.saveProperties(GUIOptions.class, props);

			props = new SBProperties();
			if (getInputFile(toolBar) != null) {
				props.put(TranslatorOptions.INPUT, getInputFile(toolBar));
			}
			props.put(TranslatorOptions.FORMAT, getOutputFileFormat(toolBar));
			SBPreferences.saveProperties(TranslatorOptions.class, props);

		} catch (BackingStoreException exc) {
			exc.printStackTrace();
			// Unimportant error... don't bother the user here.
			// GUITools.showErrorMessage(this, exc);
		}
		dispose();
		System.exit(0);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.zbit.gui.BaseFrame#getCommandLineOptions()
	 */
	@SuppressWarnings("unchecked")
	public Class<? extends KeyProvider>[] getCommandLineOptions() {
		return Translator.getCommandLineOptions().toArray(new Class[0]);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.zbit.gui.BaseFrame#getURLAboutMessage()
	 */
	public URL getURLAboutMessage() {
		return getClass().getResource("../html/about.html");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.zbit.gui.BaseFrame#getURLLicense()
	 */
	public URL getURLLicense() {
		return getClass().getResource("../html/license.html");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.zbit.gui.BaseFrame#getURLOnlineHelp()
	 */
	public URL getURLOnlineHelp() {
		return getClass().getResource("../html/help.html");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.zbit.gui.BaseFrame#getApplicationName()
	 */
	public String getApplicationName() {
		return KEGGtranslator.APPLICATION_NAME;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.zbit.gui.BaseFrame#getDottedVersionNumber()
	 */
	public String getDottedVersionNumber() {
		return KEGGtranslator.VERSION_NUMBER;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.zbit.gui.BaseFrame#getURLOnlineUpdate()
	 */
	public URL getURLOnlineUpdate() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see de.zbit.gui.BaseFrame#getFileHistoryKeyProvider()
	 */
	@Override
	public Class<? extends FileHistory> getFileHistoryKeyProvider() {
		return KEGGtranslatorHistory.class;
	}

	/* (non-Javadoc)
	 * @see de.zbit.gui.BaseFrame#getMaximalFileHistorySize()
	 */
	public short getMaximalFileHistorySize() {
		return 10;
	}
}
