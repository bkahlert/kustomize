#!/usr/bin/perl

# see https://rwmj.wordpress.com/tag/libguestfs/page/4/
# Usage: ./fs-mount.pl disk.img /tmp/fs &

use warnings;
use strict;

use Sys::Guestfs;

die "usage: $0 disk1 [disk2 ...] mountpoint\n" if @ARGV <= 1;

my $mp = pop;

my $g = Sys::Guestfs->new ();
foreach (@ARGV) {
    $g->add_drive ($_);
}
$g->launch ();

# Examine the filesystems.
my %fses = $g->list_filesystems ();

# Create the mountpoint directories (in the libguestfs namespace)
# and mount the filesystems on them.
foreach my $fs (sort keys %fses) {
    # mkmountpoint is really the same as mkdir.  Unfortunately there
    # is no 'mkdir -p' equivalent, so we have to do this instead:
    my @components = split ("/", $fs);
    for (my $i = 1; $i < @components; ++$i) {
        my $dir = "/" . join ("/", @components[1 .. $i]);
        eval { $g->mkmountpoint ($dir) }
    }

    # Don't fail if the filesystem can't be mounted, eg. it's swap.
    eval { $g->mount ($fs, $fs) }
}

# Export the filesystem on the host.
$g->mount_local ($mp);
$g->mount_local_run ();

# Close nicely since we mounted everything writable.
$g->shutdown ();
$g->close ();
