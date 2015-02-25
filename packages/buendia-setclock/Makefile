include ../Makefile.inc

# buendia-setclock is a slightly unusual; it contains source code that needs
# to be compiled on the target platform, so it Depends on gcc and performs the
# compilation as a postinst step.  An empty /usr/bin/buendia-setclock file is
# included in data.tar.gz and later overwritten with the compiled binary.

$(EXTRA_DATA): $(EXTRA_DATA)/usr/bin/buendia-setclock

$(EXTRA_DATA)/usr/bin/buendia-setclock:
	mkdir -p $$(dirname $@)
	touch $@
