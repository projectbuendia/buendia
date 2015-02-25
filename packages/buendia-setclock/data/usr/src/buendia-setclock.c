#include <errno.h>
#include <stdio.h>
#include <stdlib.h>
#include <sys/time.h>
#include <unistd.h>

/* Used to store the return code of a system command. */
long rv = 0;

/* Displays the value in rv interpreted as a system command exit code. */
void check(char* name) {
  fprintf(stderr, "%s() exit code: %ld\n", name, rv);
  if (rv != 0) {
    perror(name);
    if (errno == EPERM) fprintf(stderr, "EPERM\n");
    if (errno == EINVAL) fprintf(stderr, "EINVAL\n");
    if (errno == EFAULT) fprintf(stderr, "EFAULT\n");
  }
}

/* Displays an integer value. */
void show(char* name, long value) {
  fprintf(stderr, "%s: %ld\n", name, value);
}

/* Gets the system clock's time in seconds since 1970-01-01 00:00 UTC. */
long gettime() {
  struct timeval tv;
  gettimeofday(&tv, NULL);
  show("gettime", tv.tv_sec);
  return tv.tv_sec;
}

/* Sets the system clock given a time in seconds since 1970-01-01 00:00 UTC. */
void settime(long sec) {
  struct timeval tv;
  tv.tv_sec = sec;
  tv.tv_usec = 0;

  show("settime", tv.tv_sec);
  rv = settimeofday(&tv, NULL);
  check("settimeofday");
}

/*
 * Takes one argument, a timestamp in seconds since 1970-01-01 00:00 UTC,
 * and sets the system clock to that time.  The setclock binary should be
 * setuid root so that the tomcat7 user can use it to set the time.
 */

int main(int argc, char** argv) {
  struct timeval tv;
  long old_sec = 0;
  long new_sec = 0;

  new_sec = atol(argv[1] ? argv[1] : "0");
  show("argument", new_sec);

  old_sec = gettime();
  show("geteuid", geteuid());
  rv = seteuid(0);
  check("seteuid");
  show("geteuid", geteuid());

  settime(new_sec);
  gettime();
  show("result", rv);
  exit(rv);
}
