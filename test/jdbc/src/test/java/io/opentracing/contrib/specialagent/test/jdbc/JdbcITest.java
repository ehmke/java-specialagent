/* Copyright 2019 The OpenTracing Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.opentracing.contrib.specialagent.test.jdbc;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import io.opentracing.contrib.specialagent.TestUtil;

public class JdbcITest {
  public static void main(final String[] args) throws SQLException, ClassNotFoundException {
    System.setProperty("sa.instrumentation.plugin.jdbc.ignoreForTracing.separator", "@@@");
    System.setProperty("sa.instrumentation.plugin.jdbc.ignoreForTracing", "SELECT 1 FROM dual @@@ SELECT 2 FROM dual");

    Class.forName("org.h2.Driver");

    try (
      final Connection connection = DriverManager.getConnection("jdbc:h2:mem:jdbc");
      final Statement statement = connection.createStatement();
      final ResultSet resultSet = statement.executeQuery("show databases");
    ) {
      while (resultSet.next())
        System.out.println(resultSet.getString(1));

      statement.executeUpdate("CREATE TABLE employer (id INTEGER)");

      // should be ignored as ignoreForTracing specified, spans no change
      statement.executeQuery("SELECT 1 FROM dual");
      statement.executeQuery("SELECT 2 FROM dual");

      // not an ignored sql, spans increased
      statement.executeQuery("SELECT 3 FROM dual");

      // no more span created if no active span
      statement.executeQuery("SELECT 3 FROM dual");
      statement.executeQuery("SELECT 4 FROM dual");
    }

    TestUtil.checkSpan("java-jdbc", 6);
  }
}