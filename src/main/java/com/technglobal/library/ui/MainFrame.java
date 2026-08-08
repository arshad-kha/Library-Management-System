package com.technglobal.library.ui;

import javax.swing.*;
import java.awt.*;

public class MainFrame extends JFrame {

    public MainFrame() {
        setTitle("TechnGlobal Library Management System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 650);
        setLocationRelativeTo(null);
        setMinimumSize(new Dimension(850, 550));

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Books", new BooksPanel());
        tabs.addTab("Members", new MembersPanel());
        tabs.addTab("Transactions", new TransactionsPanel());

        add(tabs);
    }
}
