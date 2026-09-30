# Book Tracker

A Java learning project developed one small step at a time.

## Current version: one book

The program stores a sample title, author and publication year in variables,
then prints them in the console. The sample book is fictional.

## Run

Requires JDK 17 or newer. In PowerShell, from this project directory:

```powershell
New-Item -ItemType Directory -Force build | Out-Null
javac -encoding UTF-8 --release 17 -d build src/Main.java
java -cp build Main
```

Expected output:

```text
Title: Learning Java
Author: Example Author
Year: 2026
```

## Planned steps

- Read a book's details from the keyboard.
- Represent each book with a class.
- Store several books in an array.
- Add listing and search.

## Learning and authorship

This first example and documentation were written with a coding assistant
for a guided learning session. Personal exercises and later changes will be
documented as they are completed. No course solution files are included.
