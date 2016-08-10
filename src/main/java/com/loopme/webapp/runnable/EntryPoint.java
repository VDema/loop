package com.loopme.webapp.runnable;

import com.github.fakemongo.Fongo;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.servlet.GuiceFilter;
import com.google.inject.servlet.GuiceServletContextListener;
import com.loopme.webapp.dao.DbInitializer;
import com.loopme.webapp.modules.BaseModule;
import com.loopme.webapp.modules.CachModule;
import com.loopme.webapp.modules.MongoDataBaseModule;
import com.loopme.webapp.modules.SqlDataBaseModule;
import com.loopme.webapp.reader.PropertiesReader;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.h2.jdbcx.JdbcConnectionPool;

import javax.servlet.DispatcherType;
import javax.sql.DataSource;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Properties;

/**
 * Created by Volodymyr Dema. Will see.
 */
public class EntryPoint {
    private static final String CONFIGURATION_FILE_NAME = "cfg.properties";
    private static final String PROP_DATABASE_PASSWORD = "db.password";
    private static final String PROP_DATABASE_URL = "db.url";
    private static final String PROP_DATABASE_USERNAME = "db.username";
    private static final String SERVER_PORT = "port";
    private static final String CACHE_USE = "cache.use";

    private final PropertiesReader propertiesReader;

    public EntryPoint(final Properties props) {
        this.propertiesReader = new PropertiesReader(props);
    }

    void configureAndStart() throws Exception {

        Module dataBaseModule = createDataBaseModule("nosql");
        Module cacheModule = createCacheModule();

        Server server = createServer(dataBaseModule, cacheModule);

        try {
            server.start();
            server.join();
        } finally {
            if (server.isStopped()) {
                server.destroy();
            }
        }
    }

    private Module createCacheModule() {
        Boolean isCachEnabled = propertiesReader.get(CACHE_USE).asBoolean(false);

        return new CachModule(isCachEnabled);
    }

    private Server createServer(final Module...baseModules) {

        Server server = new Server(propertiesReader.get(SERVER_PORT).asInt(8080));

        // tell jetty to ask Guice about what servlet should handle the requests
        ServletContextHandler context = new ServletContextHandler(
                server, "/", ServletContextHandler.SESSIONS);
        // attach listener who instantiate our injector
        context.addEventListener(new GuiceServletContextListener() {
            @Override
            protected Injector getInjector() {
                List<Module> modules = Lists.newArrayList();

                modules.addAll(Arrays.asList(baseModules));
                modules.add(new BaseModule());

                Injector injector = Guice.createInjector(modules);

                injector.getInstance(DbInitializer.class).warmDb();

                return injector;
            }
        });

        context.addFilter(GuiceFilter.class, "/*", EnumSet.of(DispatcherType.REQUEST, DispatcherType.ASYNC));

        // DefaultServlet is at the end of the pipeline to handle what Guice didn't
        context.addServlet(DefaultServlet.class, "/*");
        return server;
    }


    private Module createDataBaseModule(final String sql) {
        switch (sql) {
            case "sql":
                final DataSource dataSource = createDataSource();
                Preconditions.checkNotNull(dataSource, "DataSource is Null. App couldn't be run without it");

                return new SqlDataBaseModule(dataSource);

            case "nosql":
                Fongo fongo = new Fongo("mongo fake server");
                return new MongoDataBaseModule(fongo.getDB("fongoname"));
            default:
                throw new IllegalArgumentException("Module type not Supported");
        }
    }

    private static Properties loadProps(String path) throws Exception {
        Properties props = new Properties();
        InputStream inputStream = EntryPoint.class.getClassLoader().getResourceAsStream(path);

        if (inputStream != null) {
            props.load(inputStream);
        } else {
            throw new FileNotFoundException(String.format("Property file '%s' not found in the classpath", path));
        }
        return props;
    }

    private DataSource createDataSource() {

        return JdbcConnectionPool.create(
                propertiesReader.get(PROP_DATABASE_URL).asString(), PROP_DATABASE_USERNAME, PROP_DATABASE_PASSWORD);
    }

    public static void main(String[] args) throws Exception {
        Properties props = loadProps(CONFIGURATION_FILE_NAME);

        new EntryPoint(props).configureAndStart();
    }
}
