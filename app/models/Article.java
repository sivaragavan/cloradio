package models;

import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import javax.persistence.Entity;
import javax.persistence.Id;

import play.Logger;
import play.db.ebean.Model;
import plugins.S3Plugin;

import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.util.json.JSONObject;

@Entity
public class Article extends Model {

	@Id
	public UUID id;
	public String url;
	public String title;
	public String text;
	public String jsonRes;
	public Integer totalLength;
	public Integer downloadLength;

	public static Finder<Long, Article> find = new Finder(Long.class,
			Article.class);

	public Article(String url, String title, String text, Integer totalLength,
			JSONObject jsonRes) {
		this.url = url;
		this.title = title;
		this.text = text;
		this.totalLength = totalLength;
		this.downloadLength = 0;
		this.jsonRes = jsonRes.toString();
	}

	public static void create(Article Article) {
		Article.save();
	}

	public static void delete(Long id) {
		find.ref(id).delete();
	}

	public static List<Article> all() {
		return find.all();
	}

	public static Article findByUrl(String url) {
		Iterator<Article> it = find.where().eq("url", url).findList()
				.iterator();
		return it.hasNext() ? it.next() : null;
	}

	public String getFileName(int index) {
		return id + "/" + index + ".wav";
	}

	public void saveAudio(int index, File file) {
		if (S3Plugin.amazonS3 == null) {
			Logger.error("Could not save because amazonS3 was null");
			throw new RuntimeException("Could not save");
		} else {
			PutObjectRequest putObjectRequest = new PutObjectRequest(
					S3Plugin.s3Bucket, getFileName(index), file);
			putObjectRequest.withCannedAcl(CannedAccessControlList.PublicRead); // public
																				// for
																				// all
			S3Plugin.amazonS3.putObject(putObjectRequest); // upload file
		}
	}

	public void updateStatus(Integer newLength) {
		this.downloadLength = newLength;
		this.update();
	}
}
