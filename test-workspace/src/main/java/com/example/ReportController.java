package com.example;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Added by the agent to satisfy "expose a sales report endpoint".
 *
 * <p>This file is the subject of module 01. It compiles, it sits in the right
 * package, it is named like a controller, and it has the method that was asked
 * for. Every deterministic check the tutorial can cheaply write says yes.
 *
 * <p>It also opens its own database connection, concatenates a SQL string from
 * request parameters, builds JSON by hand, swallows the exception, and never
 * closes anything — none of which the codebase's other controller does.
 */
public class ReportController {

    public String report(String from, String to) {
        StringBuilder json = new StringBuilder("{\"rows\":[");
        try {
            Connection connection = DriverManager.getConnection("jdbc:h2:mem:reports", "sa", "");
            Statement statement = connection.createStatement();
            ResultSet rows = statement.executeQuery(
                "SELECT day, total FROM sales WHERE day BETWEEN '" + from + "' AND '" + to + "'");
            boolean first = true;
            while (rows.next()) {
                if (!first) {
                    json.append(",");
                }
                json.append("{\"day\":\"").append(rows.getString("day"))
                    .append("\",\"total\":").append(rows.getInt("total")).append("}");
                first = false;
            }
        }
        catch (Exception ignored) {
            return "{\"rows\":[]}";
        }
        return json.append("]}").toString();
    }
}
