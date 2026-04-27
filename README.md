# Práctica 3: Sistema de Recuperación de Información con Apache Lucene

**Máster Universitario en Ingeniería Informática**
**Asignatura:** Gestión de Información en la Web (GIW)
**Universidad de Granada (UGR)**

---

## 1. Descripción del Proyecto

Este proyecto consiste en el desarrollo de un Sistema de Recuperación de Información utilizando la biblioteca Apache Lucene (versión 9.x) y el lenguaje Java 17.

El objetivo principal es procesar una colección de noticias almacenadas en un archivo de texto plano (formato CSV), estructurar esa información mediante la creación de un índice, y proporcionar una interfaz gráfica para que un usuario pueda buscar términos y obtener las noticias más relevantes ordenadas por importancia.

El software se compone de dos programas independientes:
* **Indexador:** Es una herramienta de línea de comandos que lee el archivo original, analiza el texto de cada noticia y construye un "índice invertido" en el disco duro.
* **Buscador:** Es una aplicación con interfaz gráfica que lee el índice previamente creado y permite al usuario introducir términos de búsqueda, mostrando los resultados en pantalla.

---

## 2. Conceptos Teóricos Aplicados

Para que las búsquedas sean precisas, el sistema no realiza una simple coincidencia de texto exacto, sino que aplica técnicas fundamentales de procesamiento de lenguaje natural:

* **Tokenización y Lematización (Stemming):** Antes de guardar el texto en el índice, el sistema lo divide en palabras individuales (tokens) y recorta sus sufijos para quedarse solo con la raíz de la palabra (lematización). Por ejemplo, si el texto original contiene las palabras "corriendo" o "correremos", el sistema almacena únicamente la raíz "corr". De este modo, si el usuario busca la palabra "correr", el motor encontrará el documento. Esto evita que se pierdan resultados relevantes por simples variaciones gramaticales o de conjugación.
* **Filtrado de Palabras Vacías (Stopwords):** Se ha introducido la lectura de un archivo de texto externo que contiene palabras muy comunes en el idioma español (artículos, preposiciones, conjunciones como "el", "de", "para"). El sistema descarta estas palabras tanto al crear el índice como al realizar la búsqueda. Esto tiene un doble beneficio: el tamaño del índice en el disco duro se reduce considerablemente, y la precisión de la búsqueda aumenta, ya que los resultados no se ven alterados por la repetición de palabras sin valor semántico.
* **Algoritmo de Relevancia (BM25):** Para decidir qué noticia aparece primera cuando hay múltiples resultados, Lucene utiliza un modelo matemático llamado BM25. Este modelo calcula la puntuación basándose en la frecuencia de la palabra buscada, pero introduce dos mejoras críticas: penaliza los documentos que son excesivamente largos (para favorecer textos más concisos y directos) y establece un límite en la puntuación de frecuencia (para evitar que un texto posicione injustamente en primer lugar solo por repetir la misma palabra de forma artificial).

---

## 3. Aspectos Innovadores Implementados

Más allá de los requisitos funcionales básicos exigidos, se han incorporado dos mejoras técnicas para optimizar la calidad de los resultados y la experiencia del usuario:

* **Búsqueda Ponderada (MultiField Boosting):** Se ha configurado el motor para que no evalúe todos los campos de la noticia por igual. El sistema busca simultáneamente la palabra introducida tanto en el "Cuerpo" de la noticia como en su "Categoría". Sin embargo, se ha programado para que una coincidencia en la Categoría tenga el doble de valor matemático que una coincidencia en el Cuerpo del texto. Esto asegura que, ante una búsqueda temática, los artículos clasificados explícitamente bajo ese tema tengan prioridad sobre aquellos que solo lo mencionan de pasada.
* **Resaltado Contextual (Highlighting):** En la interfaz gráfica, en lugar de mostrar siempre las primeras líneas de la noticia como resumen estático, el sistema analiza el texto original, extrae dinámicamente la oración exacta donde se encontró la coincidencia de la búsqueda y la muestra envolviendo la palabra clave entre corchetes. Esto permite al usuario comprender inmediatamente el contexto por el cual ese documento ha sido recuperado.

---

## 4. Resolución de Errores y Optimización de Código

Durante la fase de desarrollo, se identificaron y solucionaron varios problemas arquitectónicos para asegurar un rendimiento profesional:

* **Problema de Rendimiento por Lectura de Disco:** Inicialmente, el programa de búsqueda abría el directorio del índice y lo volvía a cerrar en cada clic del botón de búsqueda. Esto generaba un uso excesivo de la memoria y del disco duro.
  * *Solución:* Se reestructuró el código para que la lectura del índice se realice una única vez al arrancar la aplicación gráfica (como un patrón Singleton), manteniendo la estructura en memoria. Esto permite que las búsquedas consecutivas sean instantáneas.
* **Gestión Insegura de Recursos:** En el indexador, si ocurría un error leyendo el archivo CSV, los flujos de datos podían quedar abiertos, provocando bloqueos de archivos en el sistema operativo.
  * *Solución:* Se implementó la estructura "try-with-resources" introducida en las versiones modernas de Java. Esta estructura garantiza que todos los archivos, lectores y el propio motor de escritura de Lucene se cierren de forma segura y automática al finalizar el proceso, previniendo fugas de memoria.
* **Problema con la Inyección de Stopwords:** En las primeras pruebas, el sistema utilizaba las palabras vacías por defecto de Lucene, ignorando el archivo de texto externo exigido por los requisitos.
  * *Solución:* Se programó la lectura manual del archivo línea por línea, transformándolo en un objeto `CharArraySet` compatible con Lucene, el cual se inyecta directamente en el núcleo del `SpanishAnalyzer` para forzar su uso durante la indexación.

---

## 5. Instrucciones de Compilación y Ejecución

El proyecto está gestionado a través de Apache Maven. Para compilar y ejecutar el sistema, es necesario abrir una terminal de comandos en el directorio raíz del proyecto y seguir estos pasos:

### 5.1. Compilación del código
El siguiente comando descarga las dependencias de Lucene y compila las clases Java:

`mvn clean compile`

### 5.2. Ejecución del Indexador
El indexador requiere estrictamente la introducción de tres argumentos en el siguiente orden: ruta del archivo CSV, ruta del archivo de palabras vacías y nombre de la carpeta de destino para el índice.

`mvn exec:java -Dexec.mainClass="es.ugr.giw.Indexador" -Dexec.args="df_total.csv stopwords.txt index"`

### 5.3. Ejecución del Buscador
El buscador requiere un único argumento, correspondiente a la ruta de la carpeta donde se encuentra el índice previamente generado.

`mvn exec:java -Dexec.mainClass="es.ugr.giw.BuscadorGUI" -Dexec.args="index"`
