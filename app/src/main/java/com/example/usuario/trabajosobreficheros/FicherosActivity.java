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
import java.util.Date;

import cz.msebera.android.httpclient.Header;
import okhttp3.OkHttpClient;

public class FicherosActivity extends AppCompatActivity implements View.OnClickListener {


    private Date fecha;
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
    private Memoria miMemoria;
    private File fichero;
    private final String WEB = "http://alumno.mobi/~alumno/superior/aguilar/subidaErrores.php";
    private final String ERROR = "http://alumno.mobi/~alumno/superior/aguilar/trabajoFicheros/errores.txt";
    private final String NOMBREFICHERO = "errores.txt";
    Picasso picasso;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ficheros);
        miMemoria = new Memoria(getApplicationContext());
        fichero = new File(getApplicationContext().getFilesDir(), NOMBREFICHERO);
        edtImagenes = (EditText) findViewById(R.id.edtImagenes);
        edtFrases = (EditText) findViewById(R.id.edtFrases);
        imgImagenes = (ImageView) findViewById(R.id.imgImagenes);
        txvFrases = (TextView) findViewById(R.id.txvFrases);
        btnDescargar = (Button) findViewById(R.id.btnDescargar);
        btnDescargar.setOnClickListener(this);
        frases = new ArrayList<String>();
        rutasAImagenes = new ArrayList<String>();
        OkHttpClient client = new OkHttpClient();
        picasso = new Picasso.Builder(this)
                .downloader(new OkHttp3Downloader(client))
                .build();
        descargaFicheroErrores(ERROR);
    }
    @Override
    public void onClick(View v) {
        if (v == btnDescargar) {
            //Cuando se pulsa el boton descargar se establece el intervalo de tiempo.
            intervalo = obtenerIntervalo();

            //Si el intervalo es diferente de 0  se crean los contadores para rotar las frases y las imagenes
            if (intervalo > 0) {
                contadorImagenes = new MiContador(intervalo * 1000, (long)1000.0);
                contadorFrases = new MiContador(intervalo*1000, (long)1000.0);
                //Se cancelan los contadores para que en el caso que esten iniciados con anterioridad no se solapen los tiempos
                contadorImagenes.cancel();
                contadorFrases.cancel();
                //Se llama a los metodos para descargar los ficheros que contienen las imagenes y las frases
                descargaFicheroImagenes(edtImagenes.getText().toString());
                descargaFicheroFrases(edtFrases.getText().toString());
            }
            else {
                /*En el caso de que el intervalo sea 0 o menor es que ha ocurrido algun error en el fichero. Por lo tanto se suben
                los errores al servidor*/
                subirErrores(fichero);
            }
        }
    }

    //Metodo que descarga el fichero que contiene las rutas de las imagenes
    private void descargaFicheroImagenes(final String url)
    {
        fecha = new Date();
        turnoImagen = 0;
        rutasAImagenes.clear();
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

            //En caso de dar error la descargar del fichero, se añade el error al fichero errores.txt y se sube a la web.
            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, File file) {
                if (miMemoria.escribirInterna(NOMBREFICHERO, "Error: " + statusCode + ". Se ha producido un error en la descarga del fichero de las imagenes: " + url +"  Fecha y hora de acceso: " + fecha, true, "UTF-8" )) {
                    Toast.makeText(FicherosActivity.this, "Se ha producido un error en la descarga del fichero de las imagenes", Toast.LENGTH_SHORT).show();
                    imgImagenes.setImageResource(R.drawable.error);
                }
                subirErrores(fichero);
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
                //Se recogen los errores que puede ocurrir en el fichero descargado y se suben al servior
                } catch (FileNotFoundException e) {
                    if (miMemoria.escribirInterna(NOMBREFICHERO, "Fichero de imagenes descargado pero no encontrado. Fecha y hora de acceso: " + fecha, true, "UTF-8")){
                        Toast.makeText(FicherosActivity.this, "Fichero de imagenes descargado pero no encontrado", Toast.LENGTH_SHORT).show();
                        subirErrores(fichero);
                    }
                } catch (IOException e) {
                    if (miMemoria.escribirInterna(NOMBREFICHERO, "Error de E/S en el fichero de las imagenes. Fecha y hora de acceso: " + fecha, true, "UTF-8")){
                        Toast.makeText(FicherosActivity.this, "Error de E/S en el fichero de las imagenes", Toast.LENGTH_SHORT).show();
                        subirErrores(fichero);
                    }
                }
                catch (Exception e){
                    if (miMemoria.escribirInterna(NOMBREFICHERO, e.getMessage() + ". fecha y hora de acceso: " + fecha, true, "UTF-8")){
                        Toast.makeText(FicherosActivity.this,e.getMessage(), Toast.LENGTH_SHORT).show();
                        subirErrores(fichero);
                    }
                }
            }

            @Override
            public void onFinish() {
                pd.dismiss();
            }
        });
    }

    //Metodo que descarga el fichero que contiene las frases
    private void descargaFicheroFrases(final String url)
    {
        fecha = new Date();
        turnoFrase = 0;
        frases.clear();
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

            //En caso de dar error la descargar del fichero, se añade el error al fichero errores.txt y se sube a la web.
            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, File file) {
                if (miMemoria.escribirInterna(NOMBREFICHERO, "Error: " + statusCode + ". Se ha producido un error en la descarga del fichero de las frases: " + url + "  Fecha y hora de acceso: " + fecha, true, "UTF-8" )) {
                    Toast.makeText(FicherosActivity.this, "Se ha producido un error en la descarga del fichero de las frases", Toast.LENGTH_SHORT).show();
                    establecerFrase("");
                }
                subirErrores(fichero);
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

                    //Se recogen los errores que puede ocurrir en el fichero descargado y se suben al servior
                } catch (FileNotFoundException e) {
                    if (miMemoria.escribirInterna(NOMBREFICHERO, "Fichero de frases descargado pero no encontrado. Fecha y hora de acceso: " + fecha, true, "UTF-8")){
                        Toast.makeText(FicherosActivity.this, "Fichero de frases descargado pero no encontrado", Toast.LENGTH_SHORT).show();
                        subirErrores(fichero);
                    }
                } catch (IOException e) {
                    if (miMemoria.escribirInterna(NOMBREFICHERO, "Error de E/S en el fichero de las frases. Fecha y hora de acceso: " + fecha, true, "UTF-8")){
                        Toast.makeText(FicherosActivity.this, "Error de E/S en el fichero de las frases", Toast.LENGTH_SHORT).show();
                        subirErrores(fichero);
                    }
                }
                catch(Exception e){
                    if (miMemoria.escribirInterna(NOMBREFICHERO, e.getMessage() + ". Fecha y hora de acceso: " + fecha, true, "UTF-8")){
                        Toast.makeText(FicherosActivity.this, "Error de E/S en el fichero de las frases", Toast.LENGTH_SHORT).show();
                        subirErrores(fichero);
                    }
                }
            }

            @Override
            public void onFinish() {
                pd.dismiss();
            }
        });
    }

    private void descargaFicheroErrores(String url)
    {
        fichero.delete();
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

            //En caso de dar error la descargar del fichero, se añade el error al fichero errores.txt y se sube a la web.
            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, File file) {
                Toast.makeText(FicherosActivity.this, "Ha ocurrido un error en la descargar del fichero errores.txt. Se creará un nuevo fichero de errores.", Toast.LENGTH_SHORT).show();
            }


            @Override
            public void onSuccess(int statusCode, Header[] headers, File file) {
                Toast.makeText(FicherosActivity.this, "El fichero errores.txt se ha descargado con exito", Toast.LENGTH_SHORT).show();
                FileInputStream fis;
                try {
                    fis = new FileInputStream(file);
                    BufferedReader in = new BufferedReader(new InputStreamReader(fis));
                    String linea;
                    while ((linea = in.readLine()) != null) {
                        miMemoria.escribirInterna(NOMBREFICHERO, linea, true, "UTF-8");
                    }
                    in.close();
                    fis.close();

                }
                catch(Exception e){
                    }
            }

            @Override
            public void onFinish() {
                pd.dismiss();
            }
        });
    }

    /*Metodo que obtiene el intervalo de tiempo con el que se intercambiaran las imagenes del archivo intervalo.txt ubicado en
    res/raw*/
    private long obtenerIntervalo(){
        fecha = new Date();
        long elIntervalo = 0;
        InputStream is;
        try {
            is = getResources().openRawResource(R.raw.intervalo);
            BufferedReader in = new BufferedReader(new InputStreamReader(is));

            String linea;
            while ((linea = in.readLine()) != null) {
                elIntervalo = Long.parseLong(linea.toString());
                if (elIntervalo <= 0){
                    throw new ExcepcionNegativo();
                }
            }
            in.close();
            is.close();

            return elIntervalo;

            //En el caso en el que ocurra un error en la obtencion del intervalo se recoge y se añade al fichero errores.txt
        } catch (FileNotFoundException e) {
            if (miMemoria.escribirInterna(NOMBREFICHERO, "Fichero intervalo.txt no encontrado. Fecha y hora de acceso: " + fecha, true, "UTF-8")) {
                Toast.makeText(this, "Fichero intervalo.txt no encontrado", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            if (miMemoria.escribirInterna(NOMBREFICHERO, "Error de E/S en el fichero intervalo.txt. Fecha y hora de acceso: " + fecha, true, "UTF-8")) {
                Toast.makeText(this, "Error de E/S en el fichero intervalo.txt", Toast.LENGTH_SHORT).show();
            }
        } catch (NumberFormatException e) {
            if (miMemoria.escribirInterna(NOMBREFICHERO, "El fichero intervalo.txt tiene que contener un valor long. Fecha y hora de acceso: " + fecha, true, "UTF-8")) {
                Toast.makeText(this, "El fichero intervalo.txt tiene que contener un valor long", Toast.LENGTH_SHORT).show();
            }
        }
        catch (ExcepcionNegativo e){
            if (miMemoria.escribirInterna(NOMBREFICHERO, "El intervalo debe ser mayor que 0. Fecha y hora de acceso: " + fecha, true, "UTF-8")) {
                Toast.makeText(this, "El intervalo debe ser mayor que 0", Toast.LENGTH_SHORT).show();
            }
        }
        catch (Exception e){
            if (miMemoria.escribirInterna(NOMBREFICHERO, e.getMessage() + ". Fecha y hora de acceso: " + fecha, true, "UTF-8")) {
                Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
        return elIntervalo;
    }

    //Clase que extiende de CountDownTimer para poder sobrescribir el metodo onFinish y poder lanzar el contador cuando finalice
    public class MiContador extends CountDownTimer {
        public MiContador(long startTime, long interval) {
            super(startTime, interval);
        }

        @Override
        public void onTick(long millisUntilFinished) {
        }

        @Override
        public void onFinish() {
            /*Sobrescribo el metodo onFinish para que cuando acabe alguno de los contadores se comprueba cual es y
            se vuelve a lanzar. Con esto un contador puede funcionar aunque el otro haya dado error en la descargar de ficheros*/
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

    //Metodo que cambia la frase del txvFrases.
    private void establecerFrase(String frase){
        txvFrases.setText(frase);
    }

    //Metodo que cambia la imagen del imgImagenes mediante Picasso
    private void establecerImagen(String ruta) {

        picasso.with(this).load(ruta)
                .placeholder(R.drawable.placeholder)
                .error(R.drawable.error)
                .into(imgImagenes);
    }

    //Metodo que sube el archivo errores.txt a la carpeta trabajoFicheros del servidor alumno.mobi
    private void subirErrores(File myFile) {
        final ProgressDialog progreso = new ProgressDialog(FicherosActivity.this);
        Boolean existe = true;
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

    //Excepcion que trata el caso de que el intervalo sea 0 o negativo.
    public class ExcepcionNegativo extends Exception {
        public ExcepcionNegativo() {
            super();
        }
    }
}
