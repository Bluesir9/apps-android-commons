package fr.free.nrw.commons.upload;

import android.content.Context;
import android.util.Log;

import com.android.volley.Cache;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.JsonRequest;
import com.android.volley.toolbox.Volley;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;


import java.io.UnsupportedEncodingException;

public class MwVolleyApi {

    private static RequestQueue REQUEST_QUEUE;
    private static final Gson GSON = new GsonBuilder().create();
    private Context context;

    public MwVolleyApi(Context context) {
        this.context = context;
    }
    public void request(String apiUrl) {
        JsonRequest<QueryResponse> request = new QueryRequest(apiUrl,
                new LogResponseListener<QueryResponse>(), new LogResponseErrorListener());
        getQueue().add(request);
    }

    private synchronized RequestQueue getQueue() {
        return getQueue(context);
    }

    private static RequestQueue getQueue(Context context) {
        if (REQUEST_QUEUE == null) {
            REQUEST_QUEUE = Volley.newRequestQueue(context.getApplicationContext());
        }
        return REQUEST_QUEUE;
    }

    private static class LogResponseListener<T> implements Response.Listener<T> {
        private static final String TAG = LogResponseListener.class.getName();

        @Override
        public void onResponse(T response) {
            Log.d(TAG, response.toString());
        }
    }

    private static class LogResponseErrorListener implements Response.ErrorListener {
        private static final String TAG = LogResponseErrorListener.class.getName();

        @Override
        public void onErrorResponse(VolleyError error) {
            Log.e(TAG, error.toString());
        }
    }

    private static class QueryRequest extends JsonRequest<QueryResponse> {
        private static final String TAG = QueryRequest.class.getName();

        public QueryRequest(String url,
                            Response.Listener<QueryResponse> listener,
                            Response.ErrorListener errorListener) {
            super(Request.Method.GET, url, null, listener, errorListener);
        }

        @Override
        protected Response<QueryResponse> parseNetworkResponse(NetworkResponse response) {
            String json = parseString(response);
            //Log.d(TAG, "json=" + json);
            QueryResponse queryResponse = GSON.fromJson(json, QueryResponse.class);
            return Response.success(queryResponse, cacheEntry(response));
        }


        private Cache.Entry cacheEntry(NetworkResponse response) {
            return HttpHeaderParser.parseCacheHeaders(response);
        }


        private String parseString(NetworkResponse response) {
            try {
                return new String(response.data, HttpHeaderParser.parseCharset(response.headers));
            } catch (UnsupportedEncodingException e) {
                return new String(response.data);
            }
        }
    }

    private static class QueryResponse {
        private Query query;

        @Override
        public String toString() {
            return "query=" + query.toString();
        }
    }

    private static class Query {
        private Page [] pages;

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder("pages=" + "\n");
            for (Page page : pages) {
                builder.append(page.toString());
                builder.append("\n");
            }
            builder.replace(builder.length() - 1, builder.length(), "");
            return builder.toString();
        }
    }

    private static class Page {
        private int pageid;
        private int ns;
        private String title;
        private Category[] categories;
        private Category category;

        @Override
        public String toString() {

            StringBuilder builder = new StringBuilder("PAGEID=" + pageid + " ns=" + ns + " title=" + title + "\n" + " CATEGORIES= ");
            if (categories != null) {
                for (Category category : categories) {
                    builder.append(category.toString());
                    builder.append("\n");
                }
            }
            else {
                builder.append("no categories exist");
                builder.append("\n");
            }

            builder.replace(builder.length() - 1, builder.length(), "");
            return builder.toString();
        }
    }

        private static class Category {
            private int ns;
            private String title;

            @Override
            public String toString() {
                return " ns=" + ns + " title=" + title;
            }
        }
    }



