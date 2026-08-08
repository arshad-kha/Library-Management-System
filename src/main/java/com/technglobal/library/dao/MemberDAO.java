package com.technglobal.library.dao;

import com.technglobal.library.db.DBConnection;
import com.technglobal.library.exception.LibraryException;
import com.technglobal.library.model.Member;
import com.technglobal.library.util.ValidationUtil;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class MemberDAO {

    public void addMember(Member member) throws LibraryException {
        ValidationUtil.requireNonBlank(member.getName(), "Name");
        ValidationUtil.validateEmail(member.getEmail());
        ValidationUtil.validatePhone(member.getPhone());

        String sql = "INSERT INTO members (name, email, phone, address) VALUES (?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, member.getName().trim());
            ps.setString(2, member.getEmail().trim());
            ps.setString(3, member.getPhone());
            ps.setString(4, member.getAddress());
            ps.executeUpdate();

        } catch (SQLIntegrityConstraintViolationException e) {
            throw new LibraryException("A member with this email already exists.", e);
        } catch (SQLException e) {
            throw new LibraryException("Database error while adding member: " + e.getMessage(), e);
        }
    }

    public void updateMember(Member member) throws LibraryException {
        ValidationUtil.requireNonBlank(member.getName(), "Name");
        ValidationUtil.validateEmail(member.getEmail());
        ValidationUtil.validatePhone(member.getPhone());

        String sql = "UPDATE members SET name=?, email=?, phone=?, address=? WHERE member_id=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, member.getName().trim());
            ps.setString(2, member.getEmail().trim());
            ps.setString(3, member.getPhone());
            ps.setString(4, member.getAddress());
            ps.setInt(5, member.getMemberId());

            int rows = ps.executeUpdate();
            if (rows == 0) {
                throw new LibraryException("No member found with ID " + member.getMemberId());
            }
        } catch (SQLException e) {
            throw new LibraryException("Database error while updating member: " + e.getMessage(), e);
        }
    }

    public void deleteMember(int memberId) throws LibraryException {
        String sql = "DELETE FROM members WHERE member_id=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, memberId);
            int rows = ps.executeUpdate();
            if (rows == 0) {
                throw new LibraryException("No member found with ID " + memberId);
            }
        } catch (SQLIntegrityConstraintViolationException e) {
            throw new LibraryException("Cannot delete this member — they have transaction history.", e);
        } catch (SQLException e) {
            throw new LibraryException("Database error while deleting member: " + e.getMessage(), e);
        }
    }

    public List<Member> getAllMembers() throws LibraryException {
        return getAllMembers(null, "name");
    }

    public List<Member> getAllMembers(String searchTerm, String sortColumn) throws LibraryException {
        List<String> allowedSortColumns = List.of("name", "email", "membership_date");
        String orderBy = allowedSortColumns.contains(sortColumn) ? sortColumn : "name";

        StringBuilder sql = new StringBuilder("SELECT * FROM members");
        boolean hasSearch = searchTerm != null && !searchTerm.trim().isEmpty();
        if (hasSearch) {
            sql.append(" WHERE name LIKE ? OR email LIKE ? OR phone LIKE ?");
        }
        sql.append(" ORDER BY ").append(orderBy);

        List<Member> members = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            if (hasSearch) {
                String like = "%" + searchTerm.trim() + "%";
                ps.setString(1, like);
                ps.setString(2, like);
                ps.setString(3, like);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    members.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new LibraryException("Database error while fetching members: " + e.getMessage(), e);
        }
        return members;
    }

    public Member getMemberById(int memberId) throws LibraryException {
        String sql = "SELECT * FROM members WHERE member_id=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, memberId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        } catch (SQLException e) {
            throw new LibraryException("Database error while fetching member: " + e.getMessage(), e);
        }
        throw new LibraryException("No member found with ID " + memberId);
    }

    private Member mapRow(ResultSet rs) throws SQLException {
        Member m = new Member();
        m.setMemberId(rs.getInt("member_id"));
        m.setName(rs.getString("name"));
        m.setEmail(rs.getString("email"));
        m.setPhone(rs.getString("phone"));
        m.setAddress(rs.getString("address"));
        Date date = rs.getDate("membership_date");
        if (date != null) {
            m.setMembershipDate(LocalDate.parse(date.toString()));
        }
        return m;
    }
}
