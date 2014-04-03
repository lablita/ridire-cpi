RIDIRE Installation Manual
======

http://www.ridire.it

http://lablita.dit.unifi.it/projects/RIDIRE


Java
----

Install Java version \>=1.5

Adjust `JAVA_HOME` environment variable accordingly

JBoss
-----

http://www.jboss.org/jbossas/downloads

Install JBoss version=5.1.0GA

Make sure default listening port (8080) it’s not already used. If so,
change
`JBOSS_INSTALL/server/default/conf/bindingservice.beans/META-INF/bindings-jboss-beans.xml`

from these

```xml
  ....
  <constructor>
  <!-- The name of the set of bindings to use for this server -->  
  <parameter>${jboss.service.binding.set:ports-default}</parameter>
  <!-- The binding sets -->  
  <parameter>
  ....
```


to these

```xml
  ....
  <constructor>
  <!-- The name of the set of bindings to use for this server -->  
  <parameter>${jboss.service.binding.set:ports-01}</parameter>
  <!-- The binding sets --> 
  <parameter>
  ....
```


This change will shift all ports by 100. So the HTTP listening port will
be 8180.

Make also sure the system has a high limit on maximum open file (e.g.
ubuntu’s default 1024 is often too small).

If you are not a JBoss expert, go to
`JBOSS_INSTALL/server/default/deploy` and remove the directory
`admin-console.war`, to avoid a well-known security issue.

#### Temporary directory

RIDIRE will need of a quite big amount of space for temporary files.
Sometimes /tmp (the default temporary directory) is bound to a small
partition. If so, create a dedicated temporary folder (something like
`~/ridire_tmp/`) and modify `JBOSS_INSTALL/bin/run.conf` adding
`-Djava.io.tmpdir=/home/drwolf/ridire_tmp` at the end of `JAVA_OPTS`.

MySQL
-----

Install MySQL version \> 5.0

Create a database and assign all privileges on it to a user.
```
mysql> create database ridire default character set utf8 default collate utf8_bin;

mysql> grant all privileges on ridire.* to 'ridire'@'IPADDRESS' identified by 'secret';
```

RIDIRE EAR
----------

Move to `JBOSS_INSTALL/server/default/deploy` and unpack
it.drwolf.ridire-ear.zip file

Copy `it.drwolf.ridire-ds.xml` in `JBOSS\_INSTALL/server/default/deploy`
(not in the folder created by unzipping the package).

Change `ds.xml` file accordingly to the parameters you have set for the
DB.

Not shipped libraries
---------------------

Some of the programming libraries needed by RIDIRE cannot be shipped in
the same package for licences reasons. You have to download them by
yourself and place in
`JBOSS_INSTALL/server/default/deploy/it.drwolf.ridire-ear.ear/lib/`

1.  MySQL JDBC connector
    [http://dev.mysql.com/downloads/connector/j/](http://dev.mysql.com/downloads/connector/j/)
2.  iText
    [http://sourceforge.net/projects/itext/](http://sourceforge.net/projects/itext/)

First run
---------

Move to `JBOSS_INSTALL/bin`

Run

`$ ./run.sh -b 0.0.0.0`

The option `-b` makes JBoss accept connection from every IP, not just
localhost (this could be a security issue, be aware).

The first run, beside checking if the first installation is correct,
creates needed tables with default parameters on the DB.

Web interface is reachable at
[http://SERVER\_IP:8080/it.drwolf.ridire/](http://server_ip:8080/it.drwolf.ridire/)

If you see RIDIRE’s logo, the first run went fine.

The default user administrator has the following parameters:
```
username: admin
password: changeme
```
You may want to change it modifying the corresponding record in User
table.

Parameters
----------

Parameters are set in Parameter and CommandParameter tables. They are
just key-value records; key name should have a quite explanatory name,
anyway they will be discussed later.

To make RIDIRE aware of any change in parameters, JBoss must be
restarted.

Stopping and restarting
-----------------------

The correct way to stop JBoss5 is:
```
$ JBOSS_INSTALL/bin/shutdown.sh -S
```
This command works if JBoss was installed on the default port (`8080`).
Otherwise you have to pass other parameters to the shutdown script. E.g.
if JBoss’ ports are shifted by 100, the right command is
```
$ JBOSS_INSTALL/bin/shutdown.sh -s jnp://localhost:1199
```
HERITRIX
--------

To install the crawler, unpack the bundle heritrix-RIDIRE.tar.gz, which
is a snapshot of Heritrix v. 3.1.1, with already configured profiles and
a custom module.

Take note of the folder where you’ve unpacked the bundle and change the
following parameters accordingly:

in `CommandParameter` table:

-  `heritrix.dir`

in `Parameter` table:

- `jobs.dir`
- `localresoruces.dir`

Now the crawler is configured, but it’s not very useful, because the
pipeline is not installed. Without the pipeline, downloaded resources
won’t be processed and transformed in plain text and linguistic
information won’t br extracted.

### 

Pipeline
--------

#### Transformation to HTML

DOC, RTF, TXT conversion implementations are embedded in the
application. No configuration needed.

PDF conversion needs the following configuration steps:

1.  **pdftohtml** - you need to install pdftohtml version 0.40
    http://pdftohtml.sourceforge.net/, based on Xpdf version 3.01.
    Notice that pdftohtml version that is shipped in ubuntu’s package is
    not compatible. Change pdftohtml.bin CommandParameter accordingly.
2.  **PdfCleaner** - place `PdfCleaner.jar` in a folder of your choice. Change
    `pdfcleaner.jar` CommandParameter accordingly.

#### HTML cleaner

Place `ridirecleaner.jar` in a folder of your choice. Change
CommandParameter `ridirecleaner.jar` accordingly.

To use **Readability** you must get an API key from
[www.readability.com](http://www.readability.com)/developers/api. Read
carefully http://www.readability.com/developers/terms

To configure Readability cleaning, you need to adjust the following
Parameters:

1.  `readability.host` that must match the URL of RIDIRE application;
2.  `readability.key` with the provided key

**Alchemy** is an external web service too. You must obtain an API key
([http://www.alchemyapi.com](http://www.alchemyapi.com)). Free API key
have a limit of 1000 calls per day. You may request a key for academic
use or a commercial one (see website for details).

You must write the key in the `alchemy.key` Parameter.

Change `temp.dir` Parameter according to your system’s temporary directory
of to the one you have chosen.

#### Language detection

It’s embedded in the application. No configuration needed.

#### Treetagger

Download and install TreeTagger following instruction on the web site
[http://www.cis.uni-muenchen.de/\~schmid/tools/TreeTagger/](http://www.cis.uni-muenchen.de/~schmid/tools/TreeTagger/)

Use Marco
Baroni's [Italian parameter file](http://www.cis.uni-muenchen.de/%7Eschmid/tools/TreeTagger/data/italian-par2-linux-3.1.bin.gz)

Modify the following CommandParameters on db:

1.  `treetagger.bin`
2.  `treetagger.parfile`
3.  `treetaggerutf8.bin`

After installing the pipeline RIDIRE is ready to crawl and process resources.

PoS tagging is disabled in default installation. Set `pos.enabled` Parameter to true to enable it.

CorpusWorkbench (CWB)
---------------------

RIDIRE exploits CWB to index resources and search for concordances and collocates.


Follow installation instruction here:
<http://cwb.sourceforge.net/download.php#cwb>

CWB installation needs to be customized a little in order to cope with some tabulation problem.
After unpacking CWB package, find directory `CWB_DIR/cwb/trunk/cqp` and inside that directory, the file `output.c`.
With your text editor change line 920:

```
< fprintf(rd->stream, "\t");
---
> fprintf(rd->stream, "@@##");
```


After installing, update the following Parameters

1.  `cqp.executable`
2.  `cqp.registry`
3.  `cwb.decode.executable`
4.  `cwbscan.executable`

#### Creating a corpus

Before building a corpus you must prepare resource files in order to be
managed by CWB.

You can:

1. use a list of job names

2. use all validated jobs

This choice can be done here: <http://SERVER_IP:8080/it.drwolf.ridire/cwb.seam>

Whichever choice you make, you must provide a destination directory. If you choose 2., you must provide the list of the jobs you want to put in
the corpus.

This operation creates VRT files. For performance reasons, an *inverted* corpus is also needed.

The inverted corpus is made of the same resources with inverted order.
To make these new VRT files, you must provide the directory path of the previous step and the path of a different folder where inverted VRT
files will be placed and click on ‘Inverti VRT’ button.

Actual indexing is performed on the command line.

Choose a corpus name (from now on: `CORPUSNAME`).
```
$ cwb-encode -d CWBDATADIR -F VRTDIR -R REGISTRYDIR/CORPUSNAME -c utf8 -xsB -P pos -P easypos -P lemma -S text:0+id+url+functional+semantic+jobname
$ cwb-make -r REGISTRYDIR/ -V CORPUSNAME
```

Repeat these steps for the inverted files, placing a `INV` suffix on the `CORPUSNAME`
```
$ cwb-encode -d CWBDATADIRINV -F VRTDIRINV -R REGISTRYDIR/CORPUSNAMEINV -c utf8 -xsB -P pos -P easypos -P lemma -S text:0+id+url+functional+semantic+jobname
$ cwb-make -r REGISTRYDIR/ -V CORPUSNAMEINV
```
Remember that for `cwb-make` the `-V` parameter must be uppercase.

Now you can modify cqp.corpusname Parameter (just with `CORPUSNAME` in uppercase format).

### Frequency lists

In order to extract collocates you must precompute frequency lists.

This is a quite time consuming operation for large corpora.

Go to <http://SERVER_IP:8080/it.drwolf.ridire/cwbfrequencyList.seam> and click 'Crea Liste di Frequenza' button.

This operation updates a bunch of frequency list tables on the database.

After the computation is complete it’s possible to extract collocates
and compute sketches.

### Sketches

First choose a directory where the sketches index will be stored and
change sketch.index.location Parameter accordingly.

Then you have to choose for which lemmas you want to compute sketches.
Be aware that sketch computation is a very time consuming process that
can take weeks on corpora with tenths of millions of terms. Moreover
terms with low frequency are likely to produce not interesting sketches.
Keeping this in mind you must create 4 text files to be put in a
‘working directory’ on the server, that tell the system which lemmas
have to be treated; each of these files must contain a single lemma per
line:

1.  nome.txt
2.  aggettivo.txt
3.  avverbio.txt
4.  verbo.txt

To compute these files you can use, for example, CWB’s cwb-scan-corpus
program.

E.g.:
```
$cwb-scan-corpus -r REGISTRYDIR/ -C CORPUSNAME lemma pos=/NOUN/ | sort -nr -k 1 | cut -f2 > nome.txt

$cwb-scan-corpus -r REGISTRYDIR/ -C CORPUSNAME lemma pos=/ADJ/ | sort -nr -k 1 | cut -f2 > aggettivo.txt

$cwb-scan-corpus -r REGISTRYDIR/ -C CORPUSNAME lemma pos=/ADV.*/ | sort -nr -k 1 | cut -f2 > avverbio.txt

$cwb-scan-corpus -r REGISTRYDIR/ -C CORPUSNAME lemma pos=/VER.*/ | sort -nr -k 1 | cut -f2\> verbo.txt
```
The commands above put all nouns in the file, even single occurrences;
maybe you should cut the file for the considerations already explained.

Go to <http://SERVER_IP:8080/it.drwolf.ridire/sketchcreation.seam>

write the working dir complete path and a number for process parameter greater than 1 and lesser than 10.

Process is a number that tells the system how many threads can be launched contemporary. Multiple threads can exploit multicore or mulitprocessor architecture on some systems. You can safely write 10 even if you are not sure how many cores you have.

Click on ‘Genera sketch’ to start the process.

You will have to check once in awhile if the process is terminated by analysing JBoss logs;
```
$ tail -f JBOSS_INSTALL/server/default/server.log
```
It may be a good idea to suspend mapping process during this phase, in
order to provide more resources to the sketch creation:

go to <http://SERVER_IP:8080/it.drwolf.ridire/resourcesAdmin.seam> and click on ‘Sospendi mapping’.

When the process is terminated go back to <http://SERVER_IP:8080/it.drwolf.ridire/sketchcreation.seam> and click on ‘Chiudi indice’ to optimize and close sketches Lucene index (now you can resume mapping, by clicking on ‘Riprendi mapping’ on resourcesAdmin page).