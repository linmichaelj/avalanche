#!/bin/bash
for variable in 1 .. 10
do
	pdflatex $1.tex
done
open $1.pdf
rm $1.log
rm $1.aux
rm $1.toc
rm $1 texput.log
