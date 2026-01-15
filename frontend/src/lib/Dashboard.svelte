<script>
    import { profile, dashboardTab } from '../state.svelte.js';
    import { onMount, onDestroy } from 'svelte';
    import Payment from './Payment.svelte';
    import Cashu from './Cashu.svelte';
    import Subscription from './Subscription.svelte';
    import Profile from './Profile.svelte';
    import Appearance from './Appearance.svelte';
const LEFT_WIDTH_KEY = 'dashboardLeftWidth';

const MIN_SIDE_WIDTH = 15;
const MAX_SIDE_WIDTH = 55;

let leftColumnWidth = 10;

let isDragging = false;
let dragTarget = null;
let hasMounted = false;
let containerElement;

onMount(() => {
    if (typeof window !== 'undefined') {
      const storedLeft = parseFloat(localStorage.getItem(LEFT_WIDTH_KEY));

      if (!Number.isNaN(storedLeft)) {
        updateLeftWidth(storedLeft);
      }
    }

    hasMounted = true;
  })

onDestroy(() => {
  document.removeEventListener('mousemove', handleMouseMove);
  document.removeEventListener('mouseup', handleMouseUp);
});


function clamp(value, min, max) {
  return Math.max(min, Math.min(max, value));
}

function updateLeftWidth(value) {
  const upperBound = Math.max(0, Math.min(MAX_SIDE_WIDTH, 100 - MIN_SIDE_WIDTH));
  leftColumnWidth = clamp(value, MIN_SIDE_WIDTH, upperBound);
}

function handleMouseDown(target, e) {
  dragTarget = target;
  isDragging = true;
  e.preventDefault();
  document.addEventListener('mousemove', handleMouseMove);
  document.addEventListener('mouseup', handleMouseUp);
}

function handleMouseMove(e) {
  if (!isDragging || !containerElement || !dragTarget) return;
  
  const rect = containerElement.getBoundingClientRect();
  const containerWidth = rect.width;
  const mouseX = e.clientX - rect.left;

  if (dragTarget === 'left') {
    const newWidth = (mouseX / containerWidth) * 100;
    updateLeftWidth(newWidth);
  }
}

function handleMouseUp() {
  isDragging = false;
  dragTarget = null;
  document.removeEventListener('mousemove', handleMouseMove);
  document.removeEventListener('mouseup', handleMouseUp);
}

$: if (hasMounted && typeof window !== 'undefined') {
  localStorage.setItem(LEFT_WIDTH_KEY, String(leftColumnWidth));
}



</script>

          <div class="p-3 lg:p-5">
            <div class="space-y-0.5">
              <h2 class="text-2xl font-bold tracking-tight">Dashboard</h2>
              <p class="text-muted-foreground">
                Welcome, {$profile.name}!
              </p>
            </div>
            <div class="border-border my-6 border-t"></div>
            <div class="flex" bind:this={containerElement}>
              <aside class="mr-6" style="width: {leftColumnWidth}%; max-width: 45%;">
                <ul
                  class="uk-nav uk-nav-secondary"
                  data-uk-switcher="connect: #component-nav; animation: uk-anmt-slide-left-sm"
                >
                  
                  <li class:uk-active={$dashboardTab === 'payment'}><a href="/" on:click|preventDefault={() => dashboardTab.set('payment')}>Payment</a></li>
                  <li class:uk-active={$dashboardTab === 'cashu'}><a href="/" on:click|preventDefault={() => dashboardTab.set('cashu')}>Cashu</a></li>
                  <li class:uk-active={$dashboardTab === 'subscriptions'}><a href="/" on:click|preventDefault={() => dashboardTab.set('subscriptions')}>Subscriptions</a></li>
                  <li class:uk-active={$dashboardTab === 'profile'}><a href="/" on:click|preventDefault={() => dashboardTab.set('profile')}>Profile</a></li>                  
                  <!-- <li><a href="#">Account</a></li> -->
                  <li class:uk-active={$dashboardTab === 'appearance'}><a href="/" on:click|preventDefault={() => dashboardTab.set('appearance')}>Appearance</a></li>
                  <!-- <li><a href="#">Notifications</a></li>
                  <li><a href="#">Display</a></li> -->

                </ul>
              </aside>
              <button 
                type="button"
                class="resizer"
                class:dragging={isDragging && dragTarget === 'left'}
                on:mousedown={(e) => handleMouseDown('left', e)}
                aria-label="Resize navigation panel"
              ></button>
              <div class="flex-1 mr-6 ml-6" style="min-width: 0;">
                <ul id="component-nav" class="uk-switcher max-w-5xl mx-auto">
                  <li class="uk-active">
                    <Payment />
                  </li>
                  <li class="uk-active">
                    <Cashu />
                  </li>
                  <li class="uk-active">
                    <Subscription />
                  </li>
                  <li class="uk-active">
                    <Profile />
                  </li>
                  <li class="uk-active">
                    <Appearance />
                  </li>
                </ul>
              </div>
            </div>
          </div>

<style> 
:global(.fr-select li.uk-active > a) {
  --uk-nav-item-bg: hsl(var(--accent));
  --uk-nav-item-color: hsl(var(--accent-foreground));
}

.resizer {
  width: 2px;
  background-color: hsl(var(--border) / 0.35);
  cursor: col-resize;
  user-select: none;
  transition: background-color 0.2s;
  flex-shrink: 0;
  border: none;
  padding: 0;
  align-self: stretch;
  background-clip: padding-box;
}

.resizer:hover,
.resizer:focus-visible {
  background-color: hsl(var(--accent) / 0.6);
  outline: none;
}

.resizer.dragging {
  background-color: hsl(var(--accent));
}
</style>