## Installation

Please download the latest release from GitHub and unpack it in a folder. After installing java, you can start Celsius it with java -jar Celsius4.jar.

## Introduction

Celsius is an open source bibliographic tool and document manager. It can handle all kinds of electronic documents, from scientific papers to eBooks. Via extendable plugins, it can interface with online databases and automatically retrieve metadata. Its specialty, however, remain papers and interfacing with the bibliographic databases in the area of high-energy physics and mathematics. The standard plugins it comes with interface with arXiv.org and inspirehep.net.

Celsius is written in JAVA 15 and runs on any modern operating system. It is released under the GPL, and you're welcome to download the source and adjust it to your needs. Feedback, code improvements and additional plugins are highly welcome! 

Celsius 4 is a rewrite of Celsius 3, moving the underlying database structure from something homegrown to sqlite. This brings vast improvements in speed and allows for many new features. People/authors are now treated fully separately from items/papers, and they are identified by various properties beyond their name. 

Celsius 3 had grown rather organically from very old sources, some more than 15 years old. The code here has been comprehensively refactored to be more uniform and to adhere to reasonable coding standards. Comments are still scarce, but I hope the situation improves with time. If you need help with implementing an interesting feature, just send me an email.

The source code is set up to be edited with Netbeans version >12 and OpenJDK 15.

![Alt text](http://www.christiansaemann.de/celsius/screenshot1.png "Optional title")

## Feature requests and bug reports:

If you find a bug that's annoying you, just send me an email with a detailed report, and I'll try my best to fix it. As far as feature requests are concerned, this depends very much on my available time. But just contact me, and I'll see what I can do.

## HiDPI/Retina display support

Celsius supports HiDPI and Retina displays for all operating systems. Just start it with the parameter HiDPI, and all fonts, icons etc. will be doubled in size.

## Features

Celsius is a open source bibliographic tool and document manager with all the obvious features. Its main advantage is that it can automatically download information from the internet, and via its plugin technology, it can be adapted to any online database source. You can also specify plugins for formatting the bibliographic data. Celsius has a powerful and highly adaptable database interface with many search functions.

Celsius can manage eBooks and synchronize the category 'eBook reader' with an eBook reader. It comes with a text extracting module of epub files and it can handle the metadata contained in these files.

The standard distribution of Celsius is set up for the needs of a scientist in high-energy or mathematical physics, i.e. it comes with plugins for the arXiv-preprint server as well as the inSPIRE-database. Additionally, Celsius now contains plugins for handling files from JSTOR and several scientific journals, sheet music from both IMSLP and Mutopia, barcodes and pdf-header information. It supports epub-files and synchronization with eBook readers is possible. Further plugins for other databases might be provided by third parties in the future and published on this webpage.

The software comes with all the essential features of a bibliographic data manager, many of which can be guessed from the screenshot linked above.

* Celsius manages collections of documents ("libraries"), to which you can add both electronic documents as well as document references. You can assign bibliographic data and arbitrary further information as tag/value-pairs to each record. You can also conveniently import a whole BibTeX file.
* Documents can be sorted in a tree of categories, which is easily adjusted to your needs. You can also specify a set of rules, which allow Celsius to sort documents automatically.
* You can search the libraries in many ways (authors, titles, keywords, date, plain text of documents, etc.)
* You can manage ebooks and synchronize your eBook reader with Celsius
* Various plugins (tiny JAVA programs, which you can easily write yourself) can be added to retrieve automatically additional information from the internet and to format the output of a bibliographic record.
* You can have Celsius produce a bibliography for a whole library, for selected documents or for all documents referred to in a tex-file.
* Celsius is highly adjustable to your needs. You can add arbitrary fields to a document's entry in a database and adjust the way this information is displayed within Celsius using HTML templates.

## Downloads

Precompiled: see the releases on GitHub for now.
Manual: see the file manual.pdf in the root folder on GitHub. This still needs considerable work.
A compatible Android version is being tested at the moment and will be released very soon.

## Credits

Many of the icons are taken/adjusted from https://iconmonstr.com/ , an excellent resources for nicely designed icons.

The database engine uses the SQLite JDBC Driver from https://github.com/xerial/sqlite-jdbc , which is well documented and has sped up Celsius significantly from version 3 to version 4.
