<h1>BabelORMLibrary</h1>

<h2>Near Zero User Configuration ORM library for public use</h2>

<b>DEVELOPER: DANIEL PINNINGTON | SUPERVISOR: NABEEL MESIH</b>

Developed for final year project Bsc. Computing 


##About BabelORM

Babel has been designed to be a near zero user configuration object relational mapping library. What this means is that 
upon successful installation a user will be able to persist POJO's into their choice of RDBS (provided it is in the dependancy file). 
Using the set up instructions it is possible to achieve minimum interation with the library to persist a java object.To use Babel all you need to do is add the library to your dependency file e.g. pom.xml (Maven) or build.gradle (Gradle).


To view the source package and classes 

```git clone https://github.com/DanielPinnington97/BabelORMLibrary.git```

cd into your chosen directory and run 

```code.```

or your chosen text editor/IDE


<h1>connection pool</h1>

Babel has a pre installed connection pool called Hikari the settings for which are located in the "BabelSettings" class 

To activate use

```public static Boolean HIKARI_CONNECTION_POOL = true;```

<h2>Example</h2>

```git clone https://github.com/DanielPinnington97/babelTest.git```