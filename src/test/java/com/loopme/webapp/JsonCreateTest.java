package com.loopme.webapp;

import com.github.fakemongo.Fongo;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provider;
import com.loopme.webapp.dao.AppDao;
import com.loopme.webapp.dto.Advertise;
import com.loopme.webapp.dto.AdvertiseRequestEvent;
import com.loopme.webapp.generator.AdvertiseDbObject;
import com.loopme.webapp.generator.AdvertiseGenerator;
import com.loopme.webapp.modules.MongoDataBaseModule;
import com.mongodb.DBCollection;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

/**
 * Created by Volodymyr Dema. Will see.
 */
public class JsonCreateTest {

    static Provider<DBCollection> collectionProvider;

    static AppDao dao;

    AdvertiseGenerator generator = new AdvertiseGenerator();

    @BeforeClass
    public static void init() {
        Fongo fongo = new Fongo("mongo fake server");
        Injector injector = Guice.createInjector(new MongoDataBaseModule(fongo.getDB("fongoname")));

        dao = injector.getInstance(AppDao.class);
        collectionProvider = injector.getProvider(DBCollection.class);
    }

    @Test
    public void insertOneRecordAndRetriveItFromCollectionWithoutExclusions() {
        AdvertiseDbObject record = generator.generateRecord(1);
        String os = record.getOs().get(0);
        String country = record.getCountries().get(0);

        record.getExcludedCountries().clear();
        record.getExcludedOs().clear();

        AdvertiseRequestEvent event = new AdvertiseRequestEvent(country, os, 4);

        log("To insert: " + record);
        log("Send requestEvent: " + event);

        collectionProvider.get().insert(record.toJson());

        List<Advertise> advertises = dao.retrieveAdvertises(event);

        log(advertises);
        assertThat(advertises, hasSize(1));
    }

    @Test
    public void insertOneRecordAndRetriveItFromCollectionWithExclusionsShoudBeFiltered() {
        AdvertiseDbObject record = generator.generateRecord(1);

        String os = record.getOs().get(0);
        String country = record.getCountries().get(0);

        record.getExcludedOs().add(os);
        record.getExcludedCountries().add(country);

        AdvertiseRequestEvent event = new AdvertiseRequestEvent(country, os, 4);

        log("To insert: " + record);
        log("Send requestEvent: " + event);

        collectionProvider.get().insert(record.toJson());

        List<Advertise> advertises = dao.retrieveAdvertises(event);

        log(advertises);
        assertThat(advertises, hasSize(0));
    }

    @Test
    public void insertOneRecordAndRetriveItFromCollectionWithOneShoudNotBeFiltered() {
        AdvertiseDbObject record = generator.generateRecord(1);

        String os = record.getOs().get(0);
        String country = record.getCountries().get(0);

        record.getExcludedOs().add(os);
        record.getExcludedCountries().clear();

        AdvertiseRequestEvent event = new AdvertiseRequestEvent(country, os, 4);

        log("To insert: " + record);
        log("To insert Json: " + record.toJson());
        log("Send requestEvent: " + event);

        collectionProvider.get().insert(record.toJson());

        List<Advertise> advertises = dao.retrieveAdvertises(event);

        log(advertises);
        assertThat(advertises, hasSize(1));
    }

    public static void log(Object msg) {
        System.out.println(msg);
    }
}
