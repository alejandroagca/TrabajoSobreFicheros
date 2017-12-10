package com.example.usuario.trabajosobreficheros;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.CountDownTimer;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.jakewharton.picasso.OkHttp3Downloader;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.FileAsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.loopj.android.http.TextHttpResponseHandler;
import com.squareup.picasso.Picasso;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import cz.msebera.android.httpclient.Header;
import okhttp3.OkHttpClient;

public class FicherosActivity extends AppCompatActivity implements View.OnClickListener {

    private EditText edtImagenes;
    private EditText edtFrases;
    private ImageView imgImagenes;
    private TextView txvFrases;
    private Button btnDescargar;
    private ArrayList<String> rutasAImagenes;
    private ArrayList<String> frases;
    private long intervalo;
    private int turnoImagen;
    private int turnoFrase;
    private MiContador contadorImagenes;
    private MiContador contadorFrases;
    private final String WEB = "http://alumno.mobi/~alumno/superior/aguilar/subidaErrores.php";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ficheros);
        edtImagenes = (EditText) findViewById(R.id.edtImagenes);
        edtFrases = (EditText) findViewById(R.id.edtFrases);
        imgImagenes = (ImageView) findViewById(R.id.imgImagenes);
        txvFrases = (TextView) findViewById(R.id.txvFrases);
        obtenerIntervalo();
        btnDescargar = (Button) findViewById(R.id.btnDescargar);
        btnDescargar.setOnClickListener(this);
        contadorImagenes = new MiContador(intervalo * 1000, (long)1000.0);
        contadorFrases = new MiContador(intervalo*1000, (long)1000.0);
    }

    @Override
    public void onClick(View v) {
        if (v == btnDescargar) {
            contadorImagenes.cancel();
            contadorFrases.cancel();
            descargaFicheroImagenes(edtImagenes.getText().toString());
            descargaFicheroFrases(edtFrases.getText().toString());
        }
    }

    private void descargaFicheroImagenes(String url)
    {
        turnoImagen = 0;
        rutasAImagenes = new ArrayList<String>();
        AsyncHttpClient client = new AsyncHttpClient();
        client.get(url, new FileAsyncHttpResponseHandler(/* Context */ this) {
            ProgressDialog pd;

            @Override
            public void onStart() {
                pd = new ProgressDialog(FicherosActivity.this);
                pd.setTitle("Por favor espere...");
                pd.setMessage("AsyncHttpResponseHadler está en progreso");
                pd.setIndeterminate(false);
                pd.setCancelable(false);
                pd.show();
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, File file) {
                Toast.makeText(FicherosActivity.this, "Se ha producido un error en la descarga del fichero de las imagenes", Toast.LENGTH_SHORT).show();
            }


            @Override
            public void onSuccess(int statusCode, Header[] headers, File file) {
                Toast.makeText(FicherosActivity.this, "El fichero con las rutas a las imagenes se ha descargado con exito", Toast.LENGTH_SHORT).show();
                FileInputStream fis;
                try {
                    fis = new FileInputStream(file);
                    BufferedReader in = new BufferedReader(new InputStreamReader(fis));

                    String linea;
                    while ((linea = in.readLine()) != null) {
                        rutasAImagenes.add(linea);
                    }
                    in.close();
                    fis.close();
                    establecerImagen(rutasAImagenes.get(turnoImagen++));
                    contadorImagenes.start();

                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFinish() {
                pd.dismiss();
            }
        });
    }

    private void descargaFicheroFrases(String url)
    {
        turnoFrase = 0;
        frases = new ArrayList<String>();
        AsyncHttpClient client = new AsyncHttpClient();
        client.get(url, new FileAsyncHttpResponseHandler(/* Context */ this) {
            ProgressDialog pd;

            @Override
            public void onStart() {
                pd = new ProgressDialog(FicherosActivity.this);
                pd.setTitle("Por favor espere...");
                pd.setMessage("AsyncHttpResponseHadler está en progreso");
                pd.setIndeterminate(false);
                pd.setCancelable(false);
                pd.show();
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, File file) {
                Toast.makeText(FicherosActivity.this, "Se ha producido un error en la descarga del fichero de las frases", Toast.LENGTH_SHORT).show();
            }


            @Override
            public void onSuccess(int statusCode, Header[] headers, File file) {
                Toast.makeText(FicherosActivity.this, "El fichero con las rutas a las frases se ha descargado con exito", Toast.LENGTH_SHORT).show();
                FileInputStream fis;
                try {
                    fis = new FileInputStream(file);
                    BufferedReader in = new BufferedReader(new InputStreamReader(fis));

                    String linea;
                    while ((linea = in.readLine()) != null) {
                        frases.add(linea);
                    }
                    in.close();
                    fis.close();
                    establecerFrase(frases.get(turnoFrase++));
                    contadorFrases.start();

                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFinish() {
                pd.dismiss();
            }
        });
    }

    private void obtenerIntervalo(){
        InputStream is;

        try {
            is = getResources().openRawResource(R.raw.intervalo);
            BufferedReader in = new BufferedReader(new InputStreamReader(is));

            String linea;
            while ((linea = in.readLine()) != null) {
                intervalo = Long.parseLong(linea.toString());
            }
            in.close();
            is.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public class MiContador extends CountDownTimer {
        public MiContador(long startTime, long interval) {
            super(startTime, interval);
        }

        @Override
        public void onTick(long millisUntilFinished) {
        }

        @Override
        public void onFinish() {
            if (this.equals(contadorImagenes)) {
                if (turnoImagen == rutasAImagenes.size()) {
                    turnoImagen = 0;
                }
                establecerImagen(rutasAImagenes.get(turnoImagen++));
                contadorImagenes.start();
            }

            if (this.equals(contadorFrases)){
                if (turnoFrase == frases.size()){
                    turnoFrase = 0;
                }
                establecerFrase(frases.get(turnoFrase++));
                contadorFrases.start();
            }
        }
    }


    private void establecerFrase(String frase){
        txvFrases.setText(frase);
    }

    private void establecerImagen(String ruta) {
        OkHttpClient client = new OkHttpClient();
        Picasso picasso = new Picasso.Builder(this)
                .downloader(new OkHttp3Downloader(client))
                .build();

        picasso.with(this).load(ruta)
                .placeholder(R.drawable.placeholder)
                .error(R.drawable.error)
                .into(imgImagenes);
    }

    private void subirErrores(String fichero) {
        final ProgressDialog progreso = new ProgressDialog(FicherosActivity.this);
        File myFile;
        Boolean existe = true;
        myFile = new File(fichero);
        RequestParams params = new RequestParams();
        try {
            params.put("fileToUpload", myFile);
        } catch (FileNotFoundException e) {
            existe = false;
        }
        if (existe)
            RestClient.post(WEB, params, new TextHttpResponseHandler() {
                @Override
                public void onStart() {
                    // called before request is started
                    progreso.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                    progreso.setMessage("Conectando . . .");
                    //progreso.setCancelable(false);
                    progreso.setOnCancelListener(new DialogInterface.OnCancelListener() {
                        public void onCancel(DialogInterface dialog) {
                            RestClient.cancelRequests(getApplicationContext(), true);
                        }
                    });
                    progreso.show();
                }

                public void onSuccess(int statusCode, Header[] headers, String response) {
                    // called when response HTTP status is "200 OK"
                    progreso.dismiss();
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, String response, Throwable t) {
                    // called when response HTTP status is "4XX" (eg. 401, 403, 404)
                    progreso.dismiss();
                }
            });
    }
}
