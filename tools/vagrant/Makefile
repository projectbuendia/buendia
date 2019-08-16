all: update

initial: clean
	vagrant up testing
	vagrant snapshot save testing initial

update:
	@vagrant snapshot list testing | grep -q initial || make initial
	vagrant snapshot restore --provision testing initial
	vagrant snapshot save --force testing initial

clean:
	vagrant destroy testing
