package com.justjournal.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Caryn Holt
 */
public class FormGui implements ActionListener {

    final Logger log = LoggerFactory.getLogger(FormGui.class);

    private JFrame frame;
    private JPanel loginPanel;
    private JTextField username;
    private JPasswordField password;
    private JLabel loginProblems;

    private JTextField subject;
    private JTextArea body;
    private JTextField music;
    private JComboBox<String> location;
    private JComboBox<String> mood;
    private JCheckBox emailComments;
    private JCheckBox allowComments;
    private JCheckBox autoFormat;
    private JComboBox<String> security;

    /**
     * Constructor
     */
    public FormGui() {
        initJFrame();
    }

    /**
     * Creates JFrame
     */
    private void initJFrame() {
        frame = new JFrame("JustJournal Client");

        // add menus
        final JMenuBar jjMenuBar = new JMenuBar();

        final JMenu jjMenu = new JMenu("JustJournal");

        final JMenuItem loginMenu = new JMenuItem("Login");
        loginMenu.setActionCommand("login");
        loginMenu.addActionListener(this);

        final JMenuItem updateMenu = new JMenuItem("Update Journal");
        updateMenu.setActionCommand("update");
        updateMenu.addActionListener(this);

        final JMenuItem displayFriends = new JMenuItem("com.justjournal.client.Display Friends");

        final JMenuItem quitMenu = new JMenuItem("Quit");
        quitMenu.setActionCommand("quit");
        quitMenu.addActionListener(this);

        jjMenu.add(loginMenu);
        jjMenu.add(updateMenu);
        jjMenu.add(displayFriends);
        jjMenu.add(quitMenu);
        jjMenuBar.add(jjMenu);
        jjMenuBar.setVisible(true);
        frame.setJMenuBar(jjMenuBar);

        // init loginPanel
        initLogin();

        // set panel to visible and add to frame
        frame.getContentPane().add(loginPanel);

        // pack frame and set to visible
        frame.pack();
        frame.setVisible(true);
    }

    /**
     * Creates login panel
     */
    private void initLogin() {
        loginPanel = new JPanel(new GridBagLayout());
        // needed for postioning
        GridBagConstraints c = new GridBagConstraints();

        // initialize componenets
        final JLabel usernameLabel = new JLabel("Username:");
        c.gridx = 0;
        c.gridy = 0;
        loginPanel.add(usernameLabel, c);

        final JLabel passwordLabel = new JLabel("Password:");
        c.gridx = 0;
        c.gridy = 1;
        loginPanel.add(passwordLabel, c);

        username = new JTextField(15);
        c.gridx = 1;
        c.gridy = 0;
        c.insets = new Insets(5, 5, 5, 5);
        loginPanel.add(username, c);

        password = new JPasswordField(15);
        c.gridx = 1;
        c.gridy = 1;
        c.insets = new Insets(5, 5, 5, 5);
        loginPanel.add(password, c);

        loginProblems = new JLabel(" ");
        loginProblems.setForeground(Color.RED);
        c.gridx = 0;
        c.gridy = 2;
        c.gridwidth = 2;
        loginPanel.add(loginProblems, c);
        c.gridwidth = 1;

        // show problems as the user types, but don't complain about fields they haven't filled in yet
        final DocumentListener validator = new DocumentListener() {
            public void insertUpdate(final DocumentEvent e) {
                showLoginProblems(false);
            }

            public void removeUpdate(final DocumentEvent e) {
                showLoginProblems(false);
            }

            public void changedUpdate(final DocumentEvent e) {
                showLoginProblems(false);
            }
        };
        username.getDocument().addDocumentListener(validator);
        password.getDocument().addDocumentListener(validator);

        final JButton loginButton = new JButton("Login");
        // event for login button
        loginButton.setActionCommand("login");
        loginButton.addActionListener(this);
        c.gridx = 0;
        c.gridy = 3;
        loginPanel.add(loginButton, c);

        final JButton clearButton = new JButton("Clear");
        clearButton.setActionCommand("clear");
        clearButton.addActionListener(this);
        c.gridx = 1;
        c.gridy = 3;
        loginPanel.add(clearButton, c);

        loginPanel.setVisible(true);
    }

    /**
     * Creates update panel
     */
    private void initUpdate() {
        // disable login panel
        loginPanel.setVisible(false);

        // init panel
        final JPanel updatePanel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();

        // init and add components
        final JLabel subjectLabel = new JLabel("Subject:");
        c.gridy = 0;
        c.gridx = 0;
        updatePanel.add(subjectLabel, c);

        final JLabel bodyLabel = new JLabel("Body:");
        c.gridy = 1;
        c.gridx = 0;
        updatePanel.add(bodyLabel, c);

        final JLabel musicLabel = new JLabel("Music:");
        c.gridy = 2;
        c.gridx = 0;
        updatePanel.add(musicLabel, c);

        final JLabel locationLabel = new JLabel("Location:");
        c.gridy = 3;
        c.gridx = 0;
        updatePanel.add(locationLabel, c);

        final JLabel moodLabel = new JLabel("Mood:");
        c.gridy = 4;
        c.gridx = 0;
        updatePanel.add(moodLabel, c);

        subject = new JTextField(15);
        c.gridy = 0;
        c.gridx = 1;
        c.insets = new Insets(5, 5, 5, 5);
        updatePanel.add(subject, c);

        body = new JTextArea(10, 15);
        body.setLineWrap(true);
        body.setWrapStyleWord(false);
        c.gridy = 1;
        c.gridx = 1;
        c.insets = new Insets(5, 5, 5, 5);
        updatePanel.add(new JScrollPane(body, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER), c);

        music = new JTextField(15);
        c.gridy = 2;
        c.gridx = 1;
        c.insets = new Insets(5, 5, 5, 5);
        updatePanel.add(music, c);

        final String[] locationValue = {"Home", "School", "Work", "Other"};
        location = new JComboBox<>(locationValue);
        c.gridy = 3;
        c.gridx = 1;
        c.insets = new Insets(5, 5, 5, 5);
        updatePanel.add(location, c);

        final String[] moodValue = moodList();
        mood = new JComboBox<>(moodValue);
        c.gridy = 4;
        c.gridx = 1;
        c.insets = new Insets(5, 5, 5, 5);
        updatePanel.add(mood, c);

        final JLabel securityLabel = new JLabel("Security:");
        c.gridy = 5;
        c.gridx = 0;
        updatePanel.add(securityLabel, c);

        final String[] securityValues = {"Public", "Friends Only", "Private"};
        security = new JComboBox<>(securityValues);
        c.gridy = 5;
        c.gridx = 1;
        c.insets = new Insets(5, 5, 5, 5);
        updatePanel.add(security, c);

        allowComments = new JCheckBox("Allow Comments");
        allowComments.setSelected(true);
        c.gridy = 6;
        c.gridx = 0;
        updatePanel.add(allowComments, c);

        emailComments = new JCheckBox("Email Comments");
        c.gridy = 7;
        c.gridx = 0;
        updatePanel.add(emailComments, c);

        autoFormat = new JCheckBox("Auto Formatting");
        autoFormat.setSelected(true);
        c.gridy = 8;
        c.gridx = 0;
        updatePanel.add(autoFormat, c);

        final JButton updateButton = new JButton("Update");
        updateButton.setActionCommand("update");
        updateButton.addActionListener(this);
        c.gridy = 9;
        c.gridx = 0;
        updatePanel.add(updateButton, c);

        final JButton clearButton = new JButton("Clear");
        clearButton.setActionCommand("clear update");
        clearButton.addActionListener(this);
        c.gridy = 9;
        c.gridx = 1;
        updatePanel.add(clearButton, c);

        updatePanel.setVisible(true);

        frame.getContentPane().add(updatePanel);
        frame.pack();
    }

    /**
     * Calls login method
     *
     * @return true if login was successful
     */
    private boolean login() {
        if (!showLoginProblems(true)) {
            return false;
        }
        final Auth jjLogin = new Auth(username.getText(), new String(password.getPassword()));

        final boolean result;

        result = jjLogin.restLogin();
                //jjLogin.secureCheckAccount();
        log.info("Login successful? {}", result);
        if (!result) {
            showError("Login failed", jjLogin.getLastError());
        }

        // if login was successful, init update panel
        return result;
    }

    /**
     * Shows what the server would reject about the username and password.
     *
     * @param includeEmpty also report fields that are still empty
     * @return true if both are valid
     */
    private boolean showLoginProblems(final boolean includeEmpty) {
        final String user = Credentials.normalizeUsername(username.getText());
        final String pass = new String(password.getPassword());

        final List<String> problems = new ArrayList<>();
        if (includeEmpty || !user.isEmpty()) {
            final String problem = Credentials.usernameProblem(user);
            if (problem != null) {
                problems.add(problem);
            }
        }
        if (includeEmpty || !pass.isEmpty()) {
            final String problem = Credentials.passwordProblem(pass);
            if (problem != null) {
                problems.add(problem);
            }
        }

        // html so long messages wrap onto separate lines
        loginProblems.setText(problems.isEmpty() ? " " : "<html>" + String.join("<br>", problems) + "</html>");
        frame.pack();
        return Credentials.problems(user, pass).isEmpty();
    }

    /**
     * Overrides event handling
     *
     * @param e the action event performed
     */
    public void actionPerformed(ActionEvent e) {
        boolean result = false;
        if ("login".equals(e.getActionCommand())) {
            result = login();
            if (result) {
                initUpdate();
            }
        } else if ("clear".equals(e.getActionCommand())) {
            username.setText("");
            password.setText("");
            loginProblems.setText(" ");
        } else if ("clear update".equals(e.getActionCommand())) {
            initUpdate();
        } else if ("update".equals(e.getActionCommand())) {
            result = update();
        } else if ("quit".equals(e.getActionCommand())) {
            System.exit(0);
        }

        if (!result) {
            log.error("The requested action could not be completed");
        }
    }

    /**
     * Calls update method
     *
     * @return true if update was successful
     */
    private boolean update() {
        final Entry updateJJ = new Entry(username.getText(), new String(password.getPassword()));
        final boolean result = updateJJ.update(subject.getText(), body.getText(),
                selectedValue(mood), selectedValue(location),
                selectedValue(security), music.getText(),
                autoFormat.isSelected(), emailComments.isSelected(), allowComments.isSelected());
        if (!result) {
            showError("Entry not posted", updateJJ.getLastError());
        }
        return result;
    }

    private void showError(final String title, final String message) {
        final String text = message != null ? message : "Something went wrong. Please try again.";
        // html so long messages wrap instead of making a very wide dialog
        final String html = "<html><body style='width: 300px'>"
                + text.replace("&", "&amp;").replace("<", "&lt;") + "</body></html>";
        JOptionPane.showMessageDialog(frame, html, title, JOptionPane.ERROR_MESSAGE);
    }

    private static String selectedValue(final JComboBox<String> comboBox) {
        return comboBox.getItemAt(comboBox.getSelectedIndex());
    }

    /**
     * Generates mood list
     *
     * @return String array of mood list
     */
    private String[] moodList() {
        return Moods.titles();
    }

}
