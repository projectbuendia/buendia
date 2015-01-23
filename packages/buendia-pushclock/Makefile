include ../Makefile.inc

# buendia-pushclock is a slightly unusual; it contains source code that needs
# to be compiled on the target platform, so it Depends on gcc and performs the
# compilation as a postinst step.  An empty /usr/bin/buendia-pushclock file is
# included in data.tar.gz and later overwritten with the compiled binary.

$(EXTRA_ROOT): $(EXTRA_ROOT)/usr/bin/pushclock

$(EXTRA_ROOT)/usr/bin/pushclock:
	mkdir -p $$(dirname $@)
	touch $@
