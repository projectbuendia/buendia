include ../Makefile.inc

# The unsafe integration tests rely on having a copy of this around
$(EXTRA_DATA)/usr/share/buendia/tests/clear_server.sql:
	mkdir -p $(EXTRA_DATA)/usr/share/buendia/tests
	cp ../../tools/clear_server.sql $(EXTRA_DATA)/usr/share/buendia/tests

$(EXTRA_DATA): $(EXTRA_DATA)/usr/share/buendia/tests/clear_server.sql
