# What's the deal with OpenJDK 7?

Versions of Buendia <= 0.10.x rely on OpenMRS v1, which in turn requires
Tomcat 7 and Java 7. However, Java 7 is [not available in Debian
Stretch](https://packages.debian.org/search?keywords=openjdk-7). The trick to
[getting it working in
stretch](https://askubuntu.com/questions/761127/how-do-i-install-openjdk-7-on-ubuntu-16-04-or-higher)
involves pulling in packages from three different Debian distributions (jessie,
sid, and experimental), which requires making extensive changes to
[`/etc/apt/preferences.d`](apt/preferences.d) and
[`/etc/apt/sources.list.d`](apt/sources.list.d).

Our solution, for ease of installation elsewhere, is to pull static copies of
the necessary packages into our apt repository in
https://projectbuendia.github.com/builds/packages/java, and then to incorporate
those packages as `java` component in both the `stable` and `unstable` suites.

# How do I install the Java 7 requirements?

Configure apt to talk to our apt repository:

```
sudo apt-get install apt-transport-https
echo "deb [trusted=yes] https://projectbuendia.github.io/builds/packages unstable main java" >/etc/apt/sources.list.d/buendia.list
sudo apt-get update
```

Then you should be able to simply:

```
sudo apt-get install -y buendia-server buendia-site-test
```

You could also `apt-get install openjdk-7-jre-headless` et cetera.

# How do I update our copies of the Java 7 requirements?

We will probably never need to do this again, but if we do, the process looks
like this.

```
git clone git@github.com:projectbuendia/builds      # this will take a while, the repo is huge
sudo tools/download_java_deps builds/packages/java  # NOTE: this must run as root, since it modifies /etc/apt
tools/index_debs builds/packages unstable java
tools/index_debs builds/packages stable java
cd builds
git add packages/java packages/dists/*/java
git commit -m "Update to latest Java 7 requirements"
git push
```

