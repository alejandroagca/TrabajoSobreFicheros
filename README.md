# Trabajo Sobre Ficheros

Aplicación que descarga un fichero con rutas a imagenes y otro fichero con frases. En caso de que ocurra algun error durante la ejecución de la aplicación este error se añadira al fichero errores.txt almacenado en la memoria interna del telefono y se subira al servidor alumnos.mobi

Los ficheros se descargan con AAHC. Las rutas a las imagenes y las frases descargadas se almacenan en dos ArrayList<String>().
  
Una vez descargadas las rutas a las imagenes estas se descargan y se establecen en el imageView mediante Picasso.
  
Las imagenes y las frases son mostradas de manera rotatoria cada X tiempo. El tiempo es indicado mediante un archivo intervalo.txt almacenado en la carpeta /raw del proyecto.

Posibles mejoras:
* Optimizar la subida del fichero errores.txt.
