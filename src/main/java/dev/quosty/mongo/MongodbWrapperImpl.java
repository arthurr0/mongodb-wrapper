package dev.quosty.mongo;

import com.google.gson.Gson;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import dev.quosty.mongo.annotation.MongodbEntity;
import dev.quosty.mongo.impl.MongodbWrapper;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.List;

public class MongodbWrapperImpl implements MongodbWrapper {

    private final Gson gson;
    private final MongoClient client;
    private String database;

    public MongodbWrapperImpl(Gson gson, MongoClient client) {
        this.gson = gson;
        this.client = client;
    }

    public MongodbWrapperImpl(Gson gson, MongoClient client, String database) {
        this.gson = gson;
        this.client = client;
        this.database = database;
    }

    @Override
    public <T> void insertOne(T value) {
        this.getDatabaseCollection(value.getClass()).insertOne(Document.parse(this.gson.toJson(value)));
    }

    @Override
    public <T> void insertMany(List<T> value) {
        value.forEach(this::insertOne);
    }

    @Override
    public <T> void insertOne(String collection, T value) {
        this.getDatabase(value.getClass()).getCollection(collection).insertOne(Document.parse(this.gson.toJson(value)));
    }

    @Override
    public <T> void insertMany(String collection, List<T> value) {
        value.forEach(object -> this.insertOne(collection, object));
    }

    @Override
    public <T> void deleteOne(T value) {
        this.getDatabaseCollection(value.getClass()).findOneAndDelete(
                Filters.eq("_id", Document.parse(this.gson.toJson(value)).get("_id")));
    }

    @Override
    public <T> void deleteMany(T value, Bson filters) {
        this.getDatabaseCollection(value.getClass()).deleteMany(filters);
    }

    @Override
    public <T> void deleteOne(String collection, T value) {
        this.getDatabase(value.getClass()).getCollection(collection).findOneAndDelete(
            Filters.eq("_id", Document.parse(this.gson.toJson(value)).get("_id")));
    }

    @Override
    public <T> void deleteMany(String collection, T value, Bson filters) {
        this.getDatabase(value.getClass()).getCollection(collection).deleteMany(filters);
    }

    @Override
    public <T> void updateOne(T value) {
        this.getDatabaseCollection(value.getClass()).findOneAndUpdate(
                Filters.eq("_id", Document.parse(this.gson.toJson(value)).get("_id")),
                Document.parse(this.gson.toJson(value))
        );
    }

    @Override
    public <T> void updateOne(T value, Bson filters) {
        this.getDatabaseCollection(value.getClass()).findOneAndUpdate(filters, Document.parse(this.gson.toJson(value)));
    }

    @Override
    public <T> void updateOne(String collection, T value) {
        this.getDatabase(value.getClass()).getCollection(collection).findOneAndUpdate(
            Filters.eq("_id", Document.parse(this.gson.toJson(value)).get("_id")),
            Document.parse(this.gson.toJson(value))
        );
    }

    @Override
    public <T> void updateOne(String collection, T value, Bson filters) {
        this.getDatabase(value.getClass()).getCollection(collection).findOneAndUpdate(filters, Document.parse(this.gson.toJson(value)));
    }

    @Override
    public <T> void updateMany(List<T> value) {
        value.forEach(this::updateOne);
    }

    @Override
    public <T> void updateMany(List<T> value, Bson filters) {
        value.forEach(object -> this.updateOne(object, filters));
    }

    @Override
    public <T> void updateMany(String collection, List<T> value) {
        value.forEach(object -> updateOne(collection, object));
    }

    @Override
    public <T> void updateMany(String collection, List<T> value, Bson filters) {
        value.forEach(object -> this.updateOne(collection, object, filters));
    }

    @Override
    public <T> List<T> findMany(Class<T> clazz, Bson filters) {
        FindIterable<Document> documents = this.getDatabaseCollection(clazz)
                .find(filters);
        List<T> result = new ArrayList<>();
        for (Document document : documents) {
            T instance = this.gson.fromJson(document.toJson(), clazz);
            result.add(instance);
        }
        return result;
    }

    @Override
    public <T> T findOne(Class<T> clazz, Bson filters) {
        Document document = this.getDatabaseCollection(clazz)
                .find(filters)
                .first();
        if (document == null) {
            return null;
        }
        return this.gson.fromJson(document.toJson(), clazz);
    }

    @Override
    public <T> boolean isExists(Class<T> clazz, Bson filters) {
        return this.getDatabaseCollection(clazz).countDocuments(filters) > 0;
    }

    @Override
    public <T> T findOne(String collection, Class<T> clazz, Bson filters) {
        Document document = this.getDatabase(clazz)
            .getCollection(collection)
            .find(filters)
            .first();
        if (document == null) {
            return null;
        }
        return this.gson.fromJson(document.toJson(), clazz);
    }

    @Override
    public <T> List<T> findMany(String collection, Class<T> clazz, Bson filters) {
        FindIterable<Document> documents = this.getDatabase(clazz)
            .getCollection(collection)
            .find(filters);
        List<T> result = new ArrayList<>();
        for (Document document : documents) {
            T instance = this.gson.fromJson(document.toJson(), clazz);
            result.add(instance);
        }
        return result;
    }

    @Override
    public <T> boolean isExists(String collection, Class<T> clazz, Bson filters) {
        return this.getDatabase(clazz).getCollection(collection).countDocuments(filters) > 0;
    }

    @Override
    public MongoCollection<Document> getDatabaseCollection(Class<?> value) {
        if (this.database != null && value.isAnnotationPresent(MongodbEntity.class)) {
            return this.getMongodbClient().getDatabase(this.database).getCollection(value.getAnnotation(MongodbEntity.class).collection());
        }

        return !value.isAnnotationPresent(MongodbEntity.class) ? null : this.getMongodbClient()
                .getDatabase(value.getAnnotation(MongodbEntity.class).database())
                .getCollection(value.getAnnotation(MongodbEntity.class).collection());
    }

    @Override
    public MongoDatabase getDatabase(Class<?> value) {
        if (this.database != null) {
            return this.getMongodbClient().getDatabase(this.database);
        }

        return !value.isAnnotationPresent(MongodbEntity.class) ? null : this.getMongodbClient()
            .getDatabase(value.getAnnotation(MongodbEntity.class).database());
    }

    @Override
    public String getCollectionName(Class<?> value) {
        return !value.isAnnotationPresent(MongodbEntity.class) ? null : value.getAnnotation(MongodbEntity.class).collection();
    }

    public MongoClient getMongodbClient() {
        return this.client;
    }
}
