This package is no longer used.

buendia-pushclock was created to support a temporary workaround for the
fact that the Edison system clock does not stay running while power is off.

OpenMRS rejects all submitted encounters whose timestamp (encounter\_datetime)
is in the future with respect to the server's clock.  Even 1 millisecond in
the future is enough to trigger rejection; evidently OpenMRS was not designed
with a client-server model in mind, as it admits no possibility that the
client and server clocks might differ.  When the server clock falls behind
the tablet clocks, this validation constraint fails and all incoming
encounters are rejected.

To prevent rejection of encounter submissions, either the encounter time
has to be adjusted back to match the server time, or the server time has to
be pushed forward to match the encounter time.

Previously, the Edison system clock had no independent battery, so it would
fall behind whenever powered off.  The tablets had clocks that were much
more likely to correct, as they set their clocks from the Internet during
configuration and their batteries allow their clocks to run continously
without interruption.  Therefore, we would push the server's clock forward
as necessary to keep it ahead of any encounter\_datetime submitted from a
tablet.  As a result, the Edison server clock stays matched to the tablet
clock that is the most ahead.

Now, the server has a clock battery and is the authoritative time source
for the tablets, so instead of pushing the server's clock forward, we set
the server's clock once, administratively (see buendia-setclock), and then
use NTP to make the tablets' clocks match the server's clock.  The
buendia-pushclock package exists just as documentation of what we did,
and is no longer used in production.
