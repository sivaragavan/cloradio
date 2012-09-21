package controllers;

import play.mvc.Controller;
import play.mvc.Result;
import plugins.MongoPlugin;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;

public class Application extends Controller {

	public static Result index() throws Exception {

		DBCollection coll = MongoPlugin.db.getCollection("testCollection");

		BasicDBObject doc = new BasicDBObject();

		doc.put("name", "MongoDB");
		doc.put("type", "database");
		doc.put("count", 1);

		BasicDBObject info = new BasicDBObject();

		info.put("x", 203);
		info.put("y", 102);

		doc.put("info", info);

		coll.insert(doc);

		return ok("True");
	}

	public static Result addUser(final String url) {
		return ok("Test");
	}
}