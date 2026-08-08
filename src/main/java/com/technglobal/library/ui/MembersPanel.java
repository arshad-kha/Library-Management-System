package com.technglobal.library.ui;

import com.technglobal.library.dao.MemberDAO;
import com.technglobal.library.exception.LibraryException;
import com.technglobal.library.model.Member;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class MembersPanel extends JPanel {

    private final MemberDAO memberDAO = new MemberDAO();
    private final DefaultTableModel tableModel;
    private final JTable table;
    private final JTextField searchField = new JTextField(20);
    private final JComboBox<String> sortBox = new JComboBox<>(
        new String[]{"name", "email", "membership_date"});

    public MembersPanel() {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.add(new JLabel("Search:"));
        topPanel.add(searchField);
        topPanel.add(new JLabel("Sort by:"));
        topPanel.add(sortBox);
        JButton searchBtn = new JButton("Search");
        JButton clearBtn = new JButton("Clear");
        topPanel.add(searchBtn);
        topPanel.add(clearBtn);
        add(topPanel, BorderLayout.NORTH);

        tableModel = new DefaultTableModel(
            new Object[]{"ID", "Name", "Email", "Phone", "Address", "Joined"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new JTable(tableModel);
        table.setRowHeight(24);
        table.setAutoCreateRowSorter(true);
        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton addBtn = new JButton("Add Member");
        JButton editBtn = new JButton("Edit Selected");
        JButton deleteBtn = new JButton("Delete Selected");
        JButton refreshBtn = new JButton("Refresh");
        bottomPanel.add(addBtn);
        bottomPanel.add(editBtn);
        bottomPanel.add(deleteBtn);
        bottomPanel.add(refreshBtn);
        add(bottomPanel, BorderLayout.SOUTH);

        searchBtn.addActionListener(e -> loadMembers());
        clearBtn.addActionListener(e -> {
            searchField.setText("");
            loadMembers();
        });
        refreshBtn.addActionListener(e -> loadMembers());
        addBtn.addActionListener(e -> showMemberDialog(null));
        editBtn.addActionListener(e -> {
            Member selected = getSelectedMember();
            if (selected == null) {
                showInfo("Please select a member to edit.");
                return;
            }
            showMemberDialog(selected);
        });
        deleteBtn.addActionListener(e -> {
            Member selected = getSelectedMember();
            if (selected == null) {
                showInfo("Please select a member to delete.");
                return;
            }
            int confirm = JOptionPane.showConfirmDialog(this,
                "Delete \"" + selected.getName() + "\"? This cannot be undone.",
                "Confirm Delete", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                try {
                    memberDAO.deleteMember(selected.getMemberId());
                    loadMembers();
                } catch (LibraryException ex) {
                    showError(ex.getMessage());
                }
            }
        });

        loadMembers();
    }

    private void loadMembers() {
        try {
            String search = searchField.getText();
            String sort = (String) sortBox.getSelectedItem();
            List<Member> members = memberDAO.getAllMembers(search, sort);
            tableModel.setRowCount(0);
            for (Member m : members) {
                tableModel.addRow(new Object[]{
                    m.getMemberId(), m.getName(), m.getEmail(), m.getPhone(),
                    m.getAddress(), m.getMembershipDate()
                });
            }
        } catch (LibraryException ex) {
            showError(ex.getMessage());
        }
    }

    private Member getSelectedMember() {
        int row = table.getSelectedRow();
        if (row == -1) return null;
        row = table.convertRowIndexToModel(row);
        Member m = new Member();
        m.setMemberId((int) tableModel.getValueAt(row, 0));
        m.setName((String) tableModel.getValueAt(row, 1));
        m.setEmail((String) tableModel.getValueAt(row, 2));
        m.setPhone((String) tableModel.getValueAt(row, 3));
        m.setAddress((String) tableModel.getValueAt(row, 4));
        return m;
    }

    private void showMemberDialog(Member existing) {
        JTextField nameField = new JTextField(existing != null ? existing.getName() : "");
        JTextField emailField = new JTextField(existing != null ? existing.getEmail() : "");
        JTextField phoneField = new JTextField(existing != null ? existing.getPhone() : "");
        JTextField addressField = new JTextField(existing != null ? existing.getAddress() : "");

        JPanel panel = new JPanel(new GridLayout(0, 2, 6, 6));
        panel.add(new JLabel("Name:"));
        panel.add(nameField);
        panel.add(new JLabel("Email:"));
        panel.add(emailField);
        panel.add(new JLabel("Phone:"));
        panel.add(phoneField);
        panel.add(new JLabel("Address:"));
        panel.add(addressField);

        int result = JOptionPane.showConfirmDialog(this, panel,
            existing == null ? "Add Member" : "Edit Member",
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result != JOptionPane.OK_OPTION) return;

        try {
            if (existing == null) {
                Member member = new Member(nameField.getText().trim(), emailField.getText().trim(),
                    phoneField.getText().trim(), addressField.getText().trim());
                memberDAO.addMember(member);
            } else {
                existing.setName(nameField.getText().trim());
                existing.setEmail(emailField.getText().trim());
                existing.setPhone(phoneField.getText().trim());
                existing.setAddress(addressField.getText().trim());
                memberDAO.updateMember(existing);
            }
            loadMembers();
        } catch (LibraryException ex) {
            showError(ex.getMessage());
        }
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    private void showInfo(String message) {
        JOptionPane.showMessageDialog(this, message, "Info", JOptionPane.INFORMATION_MESSAGE);
    }
}
