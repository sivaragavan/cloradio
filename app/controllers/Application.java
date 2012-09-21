package controllers;

import models.Group;
import play.Play;
import play.mvc.Controller;
import play.mvc.Result;

import com.google.code.morphia.Datastore;
import com.google.code.morphia.Morphia;
import com.google.code.morphia.mapping.Mapper;
import com.google.code.morphia.query.UpdateOperations;
import com.mongodb.DB;
import com.mongodb.Mongo;
import com.mongodb.MongoURI;

public class Application extends Controller {

	public static Result index() throws Exception {
		Morphia morphia = new Morphia();
		Mongo mongo = new Mongo("ds037907-a.mongolab.com", 37907);
		DB db = mongo.getDB("heroku_app7757420");
		//System.out.println(db.authenticate("heroku_app7757420", "r82c6febvl4abqgn6bphg7lr1q".toCharArray()));
		Datastore ds = morphia.createDatastore(mongo, "heroku_app7757420", "heroku_app7757420", "r82c6febvl4abqgn6bphg7lr1q".toCharArray());
		ds.ensureIndexes();
		ds.ensureCaps();

		Group me = new Group();
		me.groupName = "test group";
		ds.save(me);
		Group e3 = ds.find(Group.class).get();
		UpdateOperations<Group> ops = ds.createUpdateOperations(Group.class)
				.set("groupName", "hello world");
		// UpdateOperations<Group> ops =
		// ds.createUpdateOperations(Group.class).unset("name");
		ds.update(
				ds.createQuery(Group.class).field(Mapper.ID_KEY).equal(e3.id),
				ops);
		String result = "";
		for (Group me2 : ds.find(Group.class)) {
			System.out.println(me2.groupName);
			result = me2.groupName;
		}
		return ok(result);
	}

	public static Result addUser(final String url) {
		return ok("Test");
	}
}