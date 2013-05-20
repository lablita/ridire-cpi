#!/usr/bin/perl -w
use strict;
use File::Copy;

if ( $#ARGV != 0 ) {
	die $!;
}

open INFILE, "<", $ARGV[0] or die $!;
my $origFileString = "";
my $newFileString="";
while (<INFILE>) {
	chomp();
	$origFileString.=$_;
}
close(INFILE);

##### INSERT YOUR CODE HERE #####
$newFileString=$origFileString;


# write new file only at the end of processing
open OUTFILE, ">", $ARGV[0] . ".tmp" or die $!;
print OUTFILE $newFileString . "\n";
close(OUTFILE);

