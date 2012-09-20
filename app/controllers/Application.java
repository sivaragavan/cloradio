package controllers;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;

import models.Article;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import com.amazonaws.util.json.JSONObject;

import play.Logger;
import play.libs.Comet;
import play.mvc.Controller;
import play.mvc.Result;

import com.google.common.io.ByteStreams;
import com.google.common.io.Closeables;

public class Application extends Controller {

	public static Result index() {
		return ok(views.html.player.render());
	}

	public static Result parse(final String url) {

		return ok(new Comet("parent.onEvent") {
			public void onConnected() {

				Article a = Article.findByUrl(url);

				if (a != null) {

					try {

						Logger.info("Already Processed : " + a.id + " : "
								+ a.url + " : Download Progress : "
								+ a.downloadLength);

						JSONObject jsonRes = new JSONObject(a.jsonRes);

						jsonRes.put("event_name", "parse_complete");
						jsonRes.put("id", a.id.toString());
						jsonRes.put("totalLength", a.totalLength);
						jsonRes.put("downloadedLength", a.downloadLength);

						sendMessage(jsonRes.toString());

						String text = a.text;
						String[] sentences = text.split("\\.");

						for (int i = 0; i < a.totalLength; i++) {

							String sentence = sentences[i];

							JSONObject tempJSON = new JSONObject();
							tempJSON.put("event_name", "audio_ready");
							tempJSON.put("index", i);
							tempJSON.put("text", sentence);
							tempJSON.put("id", a.id.toString());
							sendMessage(tempJSON.toString());
						}

					} catch (Exception e) {
						Logger.error("Error", e);
					} finally {
						close();
						Logger.info("completed parsing");
					}
				} else {

					Logger.info("Starting to process : " + url);

					HttpClient httpclient = new DefaultHttpClient();

					try {

						Logger.info("Parsing with Diffbot (url): " + url);

						HttpGet httpget = new HttpGet(
								"http://www.diffbot.com/api/article?token=d7d6e5f9c8b26964b45795a74465d809&url="
										+ url);
						ResponseHandler<String> responseHandler = new BasicResponseHandler();
						String responseBody = httpclient.execute(httpget,
								responseHandler);

						Logger.info("Response from Diffbot : " + responseBody);

						responseBody = responseBody
								.replaceAll("\n", "<br><br>");
						JSONObject jsonRes = new JSONObject(responseBody);
						jsonRes.put("event_name", "parse_result");

						String text = jsonRes.getString("text");
						String[] sentences = text.split("\\.");

						a = new Article(url, jsonRes.getString("title"),
								jsonRes.getString("text"), sentences.length,
								jsonRes);
						a.save();

						jsonRes.put("id", a.id.toString());
						jsonRes.put("totalLength", a.totalLength);
						jsonRes.put("downloadedLength", a.downloadLength);

						sendMessage(jsonRes.toString());

						for (int i = 0; i < sentences.length; i++) {

							String sentence = sentences[i];

							File file = File.createTempFile(a.id.toString(),
									".wav");

							Logger.info("Converting with Mary : " + sentence);

							HttpClient client = new DefaultHttpClient();
							HttpGet get = new HttpGet(
									"http://mary.dfki.de:59125/process?INPUT_TYPE=TEXT&OUTPUT_TYPE=AUDIO&AUDIO=WAVE_FILE&LOCALE=en_US&INPUT_TEXT="
											+ URLEncoder.encode(sentence));
							HttpResponse response = client.execute(get);

							InputStream data = response.getEntity()
									.getContent();
							OutputStream output = new FileOutputStream(file);
							try {
								ByteStreams.copy(data, output);
							} finally {
								Closeables.closeQuietly(output);
							}

							Logger.info("Adding to s3 : " + i + ".wav");

							a.saveAudio(i, file);
							a.updateStatus(i + 1);

							JSONObject tempJSON = new JSONObject();
							tempJSON.put("event_name", "audio_ready");
							tempJSON.put("index", i);
							tempJSON.put("text", sentence);
							tempJSON.put("id", a.id.toString());
							sendMessage(tempJSON.toString());

							file.delete();

							Logger.info("Added to s3 : " + i + ".wav");
						}

					} catch (Exception e) {
						Logger.error("Error while parsing", e);
					} finally {
						httpclient.getConnectionManager().shutdown();
						close();
						Logger.info("completed parsing");
					}
				}
			}
		});

	}
}