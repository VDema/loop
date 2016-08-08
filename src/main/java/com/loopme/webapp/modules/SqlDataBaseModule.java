package com.loopme.webapp.modules;

import com.google.common.base.Throwables;
import com.google.inject.Binder;
import com.google.inject.Module;
import com.loopme.webapp.dao.AppDao;
import com.loopme.webapp.dao.JdbcAppDao;

import javax.sql.DataSource;
import java.sql.Connection;

/**
 * Created by Volodymyr Dema. Will see.
 */
public class SqlDataBaseModule implements Module {
    private final DataSource dataSource;

    public SqlDataBaseModule(DataSource dataSource) {
        super();
        this.dataSource = dataSource;
    }

    @Override
    public void configure(Binder binder) {

        binder.bind(AppDao.class).toInstance(new JdbcAppDao());
        binder.bind(Connection.class).toProvider(() -> {
            try {
                return dataSource.getConnection();
            } catch (Exception e) {
                Throwables.propagate(e);
                return null;
            }
        });

    }
}